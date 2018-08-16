# Copyright (C) 2016 - present Instructure, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# FYI: fastlane will not display messages via abort so raise is used instead.
class GCloud

  # Bitrise must define the following app env vars in the workflow editor.
  def initialize opts={}
    @opts         = opts
    @app_name     = opts.fetch :app
    @test_targets = opts[:test_targets] || ''
    @robo         = opts[:robo]
    @annotations  = opts[:annotations]

    unless @test_targets.empty?
      raise 'Do not supply test_targets when running Robo tests' if @robo
      raise 'Test targets must start with class or package' unless @test_targets.match(/^class |^package /)
    end

    missing = []

    %w(GCLOUD_USER GCLOUD_PROJECT GCLOUD_KEY APP_APK TEST_APK).each do |env|
      missing << env unless ENV[env]
    end
    raise("Missing environment variables: #{missing.join(', ')}") unless missing.empty?
  end

  def _run_command command
    raise("nil command: #{command}") unless command
    command = command.strip.gsub(/[ ]+/, ' ') # normalize spaces
    _execute(command)
  end

  def _run_commands commands_string
    commands = commands_string.strip.split("\n")
    commands.each do |command|
      _run_command command
    end
  end

  def _install_gcloud_sdk
    # skip if gcloud is already installed
    return if `type gcloud 2>&1`.include?('gcloud is')

    execute_action('Install GCloud SDK') do
      _run_commands %q(
        echo 'debconf debconf/frontend select Noninteractive' | debconf-set-selections
        echo "deb https://packages.cloud.google.com/apt cloud-sdk-`lsb_release -c -s` main" | sudo tee -a /etc/apt/sources.list.d/google-cloud-sdk.list
        curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -
        sudo apt-get update -qq
        sudo apt-get install -y -qq google-cloud-sdk > /dev/null
      )
    end
  end

  def _auth_gcloud
    execute_action('Authenticate to GCloud') do
      _run_commands %q(
        echo $GCLOUD_KEY | base64 --decode > "$HOME/gcloudkey.json"
        gcloud config set project "$GCLOUD_PROJECT"
        gcloud auth activate-service-account --key-file "$HOME/gcloudkey.json" "$GCLOUD_USER"
     )
    end
  end

  def run_tests
    _install_gcloud_sdk
    _auth_gcloud

    gcloud_log = join(repo_root_dir, 'gcloud.log')
    # ensure we don't parse from an old log on Bitrise due to the cache
    FileUtils.rm_rf gcloud_log

    # gcloud gcloud firebase test android devices list
    # NexusLowRes is the fastest virtual device. larger screen size = slower
    # SoSeedy will break if we run on more than one device.
    @device_ids = %w[NexusLowRes]
    @api_level  = 26

    # list devices: gcloud firebase test android models list
    if @robo
      type = '--type robo'
    else
      type         = '--type instrumentation'
      test_apk     = %Q(--test "#{ENV['TEST_APK']}")
      sd_card_path = '--directories-to-pull=/sdcard'
    end

    flags = [
        type,
        %Q(--app "#{ENV['APP_APK']}"),
        test_apk,
        "--results-bucket android-#{@app_name}",
        "--device-ids #{@device_ids.join(',')}",
        "--os-version-ids #{@api_level}",
        '--locales en',
        '--orientations portrait',
        '--timeout 60m',
        sd_card_path
    ].reject &:nil?

    flags << %Q(--test-targets "#{@test_targets}") unless @test_targets.empty?

    # must use custom env separator or gcloud CLI will get confused on comma separated annotations
    if @opts[:coverage] || @annotations
      env_vars      = []
      env_vars      += ['coverage=true', 'coverageFile=/sdcard/coverage.ec'] if @opts[:coverage]
      env_vars      += ["annotation=#{@annotations}"] if @annotations
      env_separator = ':'
      flags << "--environment-variables ^#{env_separator}^#{env_vars.join(env_separator)}"
    end

    flags.map! {|flag| "  #{flag}"}

    command = [
        'unbuffer',
        ' gcloud firebase test android run',
        flags,
        "2>&1 | tee #{gcloud_log}"
    ].flatten

    # "unbuffer \\ \n gcloud firebase test android run -- invalid. must not have space after \\
    # "unbuffer \\\n gcloud firebase test android run  -- valid
    command = command.join(" \\\n").strip

    # https://cloud.google.com/sdk/gcloud/reference/beta/test/android/run
    execute_action('Run GCloud test') do
      _run_command command
    end

    # todo: fix coverage for Teacher app
    if @opts[:coverage]
      execute_action('Download firebase.ec') do
        raise('gcloud.log not found') unless File.exist?(gcloud_log)

        gcloud_log  = File.read gcloud_log
        match       = gcloud_log.match /GCS bucket at \[(.*)\]/
        bucket_link = match[1] if match && match.length == 2
        raise('Failed to parse GCS bucket from GCloud log!') unless bucket_link

        # note that firebase.ec must match the path expected by the firebaseJacoco gradle task
        # also the path changes depending on what device/API levels are used to run the tests
        bucket = 'gs:/' + bucket_link.split('storage/browser').last
        bucket = "#{bucket}#{@device_ids.first}-#{@api_level}-en-portrait/artifacts/coverage.ec"
        _run_command "gsutil cp #{bucket} /bitrise/src/#{@app_name}/app/build/firebase.ec"
      end

      # Generate Jacoco report using firebase.ec
      gradlew("-p #{@app_name} -Pcoverage firebaseJacoco")
    end
  end
end
