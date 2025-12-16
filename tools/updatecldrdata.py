#!/usr/bin/python3 -B
# Copyright 2018 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Regenerates (just) ICU data source files used to build ICU data files."""

from __future__ import print_function

import os
import shutil
import subprocess
import sys
from pathlib import Path

import i18nutil
import icuutil


# Run with no arguments from any directory, with no special setup required.
# See icu/docs/processes/cldr-icu.md  for the upstream ICU instructions.
def main():
  if subprocess.call(["which", "mvn"]) != 0 or subprocess.call(["which", "ant"]) != 0:
    print("Can't find the required tools. Run `sudo apt-get install maven ant` to install")
    exit(1)

  if not os.path.exists(os.path.join(Path.home(), ".m2/settings.xml")):
    print("Can\'t find `~/.m2/settings.xml`. Please follow the instructions at "
          "http://cldr.unicode.org/development/maven to create one and the github token.")
    exit(1)

  icu_dir = icuutil.icuDir()
  print('Found icu in %s ...' % icu_dir)
  cldr_dir = icuutil.cldrDir()
  print('Found cldr in %s ...' % cldr_dir)

  # 1a. Setup environment variables for all subshell
  os.environ['ANT_OPTS'] = '-Xmx8192m'
  os.environ['MAVEN_ARGS'] = '--no-transfer-progress'

  # Ant doesn't have any mechanism for using a build directory separate from the
  # source directory so this build script creates a temporary directory and then
  # copies all necessary ICU4J and CLDR source code to here before building it:
  i18nutil.SwitchToNewTemporaryDirectory()
  build_dir = os.getcwd()
  cldr_build_dir = os.path.join(build_dir, 'cldr')
  icu4c_build_dir = os.path.join(build_dir, 'icu4c')
  icu4j_build_dir = os.path.join(build_dir, 'icu4j')
  icu_tools_build_dir = os.path.join(build_dir, 'tools')

  # 1b. CLDR variables
  cldr_tmp_dir = os.path.join(build_dir, 'cldr-staging')

  # This is the location of the original CLDR source tree (not the temporary
  # copy of the tools source code) from where the data files are to be read:
  os.environ['CLDR_DIR'] = cldr_build_dir  # os.path.join(os.getcwd(), 'cldr')
  os.environ['CLDR_TMP_DIR'] = cldr_tmp_dir
  cldr_production_tmp_dir = os.path.join(cldr_tmp_dir, 'production')
  os.environ['CLDR_DATA_DIR'] = cldr_production_tmp_dir

  # 1c. ICU variables
  os.environ['ICU_DIR'] = build_dir

  os.environ['ICU4C_ROOT'] = icu4c_build_dir
  os.environ['ICU4J_ROOT'] = icu4j_build_dir
  os.environ['TOOLS_ROOT'] = icu_tools_build_dir

  print('Copying CLDR source code ...')
  shutil.copytree(cldr_dir, cldr_build_dir, symlinks=True)
  print('Copying ICU4C source code ...')
  shutil.copytree(os.path.join(icu_dir, 'icu4c'), icu4c_build_dir, symlinks=True)
  print('Copying ICU4J source code ...')
  shutil.copytree(os.path.join(icu_dir, 'icu4j'), icu4j_build_dir, symlinks=True)
  print('Copying ICU tools source code ...')
  shutil.copytree(os.path.join(icu_dir, 'tools'), icu_tools_build_dir, symlinks=True)

  # Step 3a from cldr-icu.md: Copy latest relevant CLDR dtds to ICU
  print('Copying CLDR DTDs...')
  dtd_dest_dir = os.path.join(icu4c_build_dir, 'source/data/dtd/cldr/common/dtd/')
  os.makedirs(dtd_dest_dir, exist_ok=True)
  shutil.copy(os.path.join(cldr_build_dir, 'common/dtd/ldml.dtd'), dtd_dest_dir)
  shutil.copy(os.path.join(cldr_build_dir, 'common/dtd/ldmlICU.dtd'), dtd_dest_dir)

  # 3b and 3c. Align CLDR dependency versions
  print('Aligning dependency versions...')
  # Get real versions
  real_icu_ver = subprocess.check_output([
     'mvn', 'help:evaluate', '-Dexpression=project.version', '-q', '-DforceStdout',
     '-f', icu4j_build_dir
  ]).decode('utf-8').strip()
  print('real_icu_ver: %s' % real_icu_ver)
  real_cldr_ver = subprocess.check_output([
     'mvn', 'help:evaluate', '-Dexpression=project.version', '-q', '-DforceStdout',
     '-f', os.path.join(cldr_build_dir, 'tools')
  ]).decode('utf-8').strip()
  print('real_cldr_ver: %s' % real_cldr_ver)
  # Set dependency versions
  cldr_to_icu_pom_dir = os.path.join(icu_tools_build_dir, 'cldr', 'cldr-to-icu')
  cldr_tools_pom_dir = os.path.join(cldr_build_dir, 'tools')
  subprocess.check_call([
     'mvn', 'versions:set-property', '-DgenerateBackupPoms=false',
     '-Dproperty=icu4j.version', '-DnewVersion=' + real_icu_ver,
     '-f', cldr_to_icu_pom_dir
  ])
  subprocess.check_call([
    'mvn', 'versions:set-property', '-DgenerateBackupPoms=false',
    '-Dproperty=cldr-code.version', '-DnewVersion=' + real_cldr_ver,
    '-f', cldr_to_icu_pom_dir
  ])
  subprocess.check_call([
    'mvn', 'versions:set-property', '-DgenerateBackupPoms=false',
    '-Dproperty=icu4j.version', '-DnewVersion=' + real_icu_ver,
    '-f', cldr_tools_pom_dir
  ])

  # Build ICU4J. This is required because the cldr-to-icu tool (used in step 5b) depends on the
  # ICU4J artifacts being available in the local Maven repository.
  # This step comes from: tools/cldr/cldr-to-icu/README.md
  print('Installing ICU4J tools ...')
  os.chdir(build_dir)
  subprocess.check_call(['mvn', 'clean', 'install', '-f', 'icu4j', '-DskipTests', '-DskipITs'])
  # 4. Build the CLDR library
  print('Installing CLDR tools ...')
  os.chdir(cldr_build_dir)
  subprocess.check_call([
    'mvn', 'clean', 'install', '-pl', ':cldr-all,:cldr-code',
    '-DskipTests', '-DskipITs'
  ])

  # 5a. Generate CLDR production data
  print('Building ICU data...')
  icu4c_data_build_dir = os.path.join(icu4c_build_dir, 'source/data')
  os.chdir(icu4c_data_build_dir)
  subprocess.check_call(['ant', 'cleanprod'])
  subprocess.check_call(['ant', 'setup'])
  subprocess.check_call(['ant', 'proddata'])


  # 5b. Build the new ICU4C data files. This step comes from tools/cldr/cldr-to-icu/README.md.
  os.chdir(os.path.join(icu_tools_build_dir, 'cldr', 'cldr-to-icu'))
  subprocess.check_call([
    'mvn',
    'clean',
    'package',
    '-DskipTests',
    '-DskipITs'
  ])
  subprocess.check_call([
    'java',
    '-jar',
    'target/cldr-to-icu-1.0-SNAPSHOT-jar-with-dependencies.jar',
    '--cldrDataDir=' + cldr_production_tmp_dir,
    '--includePseudoLocales'
  ])

  # 5c. Update the CLDR testData files needed by ICU4C/J tests
  icu_tools_cldr_dir = os.path.join(icu_tools_build_dir, 'cldr')
  os.chdir(icu_tools_cldr_dir)
  subprocess.check_call([
    'ant',
    'copy-cldr-testdata',
  ])

  #Step 5e from cldr-icu.md: Manually re-add the lstm entries
  print("Adding LSTM entries to brkitr/root.txt...")
  brkitr_root_txt_path = os.path.join(icu4c_build_dir, 'source/data/brkitr/root.txt')
  with open(brkitr_root_txt_path, 'r') as f:
    content = f.read()
  last_brace_pos = content.rfind('}')
  if last_brace_pos != -1:
    lstm_block = """    lstm{
       Thai{"Thai_graphclust_model4_heavy.res"}
       Mymr{"Burmese_graphclust_model5_heavy.res"}
     }
    """
  # Insert before the last brace
  new_content = content[:last_brace_pos] + lstm_block + '\n' + content[last_brace_pos:]
  with open(brkitr_root_txt_path, 'w') as f:
    f.write(new_content)

  # Copy the generated data files from the temporary directory into AOSP.
  icu4c_data_source_dir = os.path.join(icu_dir, 'icu4c/source/data')
  rmAndCopyTree(icu4c_data_build_dir, icu4c_data_source_dir)

  # Copy test data. It mirrors the copy-cldr-testdata steps in tools/cldr/build.xml.
  rmAndCopyTree(
    os.path.join(icu4c_build_dir, 'source/test/testdata/cldr'),
    os.path.join(icu_dir, 'icu4c/source/test/testdata/cldr'))
  rmAndCopyTree(
    os.path.join(icu4j_build_dir, 'main/core/src/test/resources/com/ibm/icu/dev/data/cldr'),
    os.path.join(icu_dir, 'icu4j/main/core/src/test/resources/com/ibm/icu/dev/data/cldr'))

  # Copy the generated localefallback_data.h and LocaleFallbackData.java
  shutil.copy(
    os.path.join(icu4c_build_dir, 'source/common/localefallback_data.h'),
    os.path.join(icu_dir, 'icu4c/source/common/localefallback_data.h'))
  shutil.copy(
    os.path.join(icu4j_build_dir,
                 'main/core/src/main/java/com/ibm/icu/impl/LocaleFallbackData.java'),
    os.path.join(icu_dir, 'icu4j/main/core/src/main/java/com/ibm/icu/impl/LocaleFallbackData.java'))

  print('Look in %s for new data source files' % icu4c_data_source_dir)
  sys.exit(0)


def rmAndCopyTree(src, dst):
  if os.path.exists(dst):
    shutil.rmtree(dst)
  shutil.copytree(src, dst)


if __name__ == '__main__':
  main()
