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

def join *args
  File.expand_path(File.join(*args))
end

# Location where commands are executed. Root folder of the repo.
def repo_root_dir
  @repo_root_dir = File.expand_path(File.join(__dir__, '..', '..'))
end

def bitrise
  @bitrise ||= !!ENV['BITRISE_IO']
end

def bitrise_deploy_dir
  @bitrise_deploy_dir ||= ENV['BITRISE_DEPLOY_DIR']
end

def gradle_home
  @gradle_home ||= join(__dir__, '..', '..', 'gradle')
end

def gradlew_path
  @gradlew_path ||= join(gradle_home, 'gradlew')
end

ENV['FL_GRADLE_PATH'] = gradlew_path

def google_play_key
  @google_play_key ||= join(__dir__, '..', 'android-keys', 'google_play_key.json')
end

def apps
  @apps ||= [
      'teacher',
      'parent',
      'candroid',
      'polling',
      'InstrumentationTests'
  ]
end

def app_libraries
  @app_libraries ||= [
      'annotations',
      'blueprint',
      'canvas-api',
      'canvas-api-2',
      'espresso',
      'login-api',
      'login-api-2',
      'pandautils',
      'recyclerview',
      'rceditor',
      'interactions'
  ]
end
