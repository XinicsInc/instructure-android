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

require 'digest'

class Teacher
  @coverage = false

  class << self
    attr_writer :coverage

    def app
      'teacher'
    end

    def gradle_flags
      "-p teacher#{@coverage ? ' -Pcoverage' : ''}"
    end

    def gradle_clean
      gradlew("#{gradle_flags} clean")
    end

    def gradle_build_robo_app_apk
      gradlew("#{gradle_flags} :app:assembleRoboDebug")
    end

    def gradle_build_app_apk
      gradlew("#{gradle_flags} :app:assembleQaDebug")
    end

    def gradle_build_test_apk
      gradlew("#{gradle_flags} :app:assembleQaDebugAndroidTest")
    end

    def install_app_apk
      # adb install -r does not work if the apk signatures do not match
      _execute("adb uninstall #{package}") rescue nil
      _execute("adb install #{app_apk_path}")
    end

    def install_test_apk
      # adb install -r does not work if the apk signatures do not match
      _execute("adb uninstall #{package}.test") rescue nil
      _execute("adb install #{test_apk_path}")
    end

    def package
      'com.instructure.teacher'
    end

    def ui_package
      package + '.ui'
    end

    def runner
      "#{package}.test/#{package}.ui.utils.TeacherInstructureRunner"
    end

    def update_device_app_apk
      gradle_build_app_apk
      return if MD5.apk_already_installed?(app_apk_path, device_app_apk_path)
      install_app_apk
    end

    def to_s
      'teacher'
    end

    private

    def app_apk_path
      '/bitrise/deploy/app-qa-debug.apk'
    end

    def device_app_apk_path
      "/data/app/#{package}-1/base.apk"
    end

    def test_apk_path
      '/bitrise/deploy/app-qa-debug-androidTest.apk'
    end
  end
end
