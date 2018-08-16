# Copyright (C) 2017 - present Instructure, Inc.
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
require 'excon'

class SmartBuildUtils
  class << self
    # Checks if the given directory differs between the current branch and the specified branch
    def changed? directory, dest_branch
      cmd_diff = "git diff origin/#{dest_branch} --shortstat '#{directory}'"
      result = Fastlane::Actions.sh(cmd_diff, log: false).chomp rescue ''
      changed = !result.empty?
      ui_important "Change detected in #{directory}" if changed
      changed
    end

    def get_env env_key
      ::ENV[env_key] || raise("Missing ENV ${env_key}")
    end

    # Checks if an app can be skipped due to having no changes. Only works on Bitrise
    def skippable? app
      app = app.to_s
      raise "Unrecognized app '#{app}'." unless apps.include? app
      ui_message "Scanning for changes in '#{app}' project"

      # Get destination branch, which is only specified for pull requests
      dest_branch = get_env 'BITRISEIO_GIT_BRANCH_DEST' rescue nil
      return false if dest_branch.nil? || dest_branch.empty?

      # Fetch the destination branch for comparison
      cmd_fetch = "git fetch origin #{dest_branch}"
      Fastlane::Actions.sh(cmd_fetch, log: false).chomp rescue nil

      # Do not skip for changes in gradle, fastlane, buildSrc, or private data dirs
      return false if changed? join('..', 'gradle'), dest_branch
      return false if changed? join('..', 'fastlane'), dest_branch
      return false if changed? join('..', 'buildSrc'), dest_branch
      return false if changed? join('..', 'private-data', app), dest_branch

      # Do not skip for changes in the app itself
      return false if changed? join('..', app), dest_branch

      # Get libs referenced by the app
      settings_gradle = File.read(join('..', app, 'settings.gradle'))
      check_libs      = settings_gradle.scan(/(?<=:).*?(?=['"])/).uniq & app_libraries

      # Do not skip for changes in any referenced lib
      check_libs.each do |lib|
        return false if changed? join('..', lib), dest_branch
      end

      # Assume the app build can be skipped at this point
      # Set SKIP_BUILD env variable so it can be accessed by the 'run_if' check in subsequent bitrise steps
      ui_success "No changes detected in #{app} or any of its dependencies. Skipping build."

      bitrise_app_slug = get_env 'BITRISE_APP_SLUG'
      bitrise_build_slug = get_env 'BITRISE_BUILD_SLUG'
      bitrise_token = get_env 'BITRISE_TOKEN'
      abort_url = "https://api.bitrise.io/v0.1/apps/#{bitrise_app_slug}/builds/#{bitrise_build_slug}/abort"

      connection = Excon.post(abort_url,
        headers: { 
          'Authorization' => "token #{bitrise_token}",
          'Content-Type' => 'application/json; charset=UTF-8'
        },
        body: { 
          abort_reason: "Build skipped. No changes detected.",
          skip_notifications: true,
          abort_with_success: true
        }.to_json
      )

      system("envman add --key SKIP_BUILD --value 'true'")
      true
    end
  end
end
