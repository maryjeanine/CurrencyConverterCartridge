freeStyleJob("act1") {

    // general
    properties {
        copyArtifactPermissionProperty {
        projectNames('act3')
        }
    }  

    // source code management  
    scm {
        git {
            remote {
                url('git@gitlab:Atienza/CurrencyConverterDTS.git')
                credentials('07971668-583b-4135-8473-70e928502555')
            }
        }
    }

    // build triggers
     triggers {
        gitlabPush {
            buildOnMergeRequestEvents(true)
            buildOnPushEvents(true)
            enableCiSkip(false)
            setBuildDescription(false)
            rebuildOpenMergeRequest('never')
        }
    }
    wrappers {
        preBuildCleanup()
    }
  
    // build
    steps {
        maven{
            mavenInstallation('ADOP Maven')
            goals('package')
        }
    }

    // post build actions
     publishers {
        archiveArtifacts {
            pattern('**/*.war')
            onlyIfSuccessful()
        }
        downstream('act2', 'SUCCESS')
    }
}

freeStyleJob("act2") {
    // source code management  
    scm {
        git {
            remote {
                url('git@gitlab:Atienza/CurrencyConverterDTS.git')
                credentials('07971668-583b-4135-8473-70e928502555')
            }
        }
    }

    // build
    configure { project ->
        project / 'builders' / 'hudson.plugins.sonar.SonarRunnerBuilder' {
        properties('''
            sonar.projectKey=SonarActivityTest
            sonar.projectName=simulationActivity
            sonar.projectVersion=1.0
            sonar.sources=.
        ''')
        javaOpts()
        jdk('(Inherit From Job)')
        task()
        }
    }    

  //post build actions
  publishers {
        downstream('act3', 'SUCCESS')
    }

}

freeStyleJob("act3") {

  // build copy artifacts

steps {
      copyArtifacts('act1') {
            includePatterns('target/*.war')
            buildSelector {
                latestSuccessful(true)
            }
            fingerprintArtifacts()
        }
      nexusArtifactUploader {
        nexusVersion('NEXUS2')
        protocol('http')
        nexusUrl('nexus:8081/nexus')
        groupId('DTSActivity')
        version('1')
        repository('snapshots')
        credentialsId('07971668-583b-4135-8473-70e928502555')
        artifact {
            artifactId('CurrencyConverter')
            type('war')
            file('/var/jenkins_home/jobs/act3/workspace/target/CurrencyConverter.war')
        }
      }
    }
  
  // post build actions
     publishers {
        archiveArtifacts {
            pattern('**/*.war')
            onlyIfSuccessful()
        }
        downstream('act4', 'SUCCESS')
    }
}

freeStyleJob("act4") {

  // label
  label('ansible')

  // source code management  
    scm {
        git {
            remote {
                url('http://13.57.143.226/gitlab/Atienza/Ansible.git')
                credentials('07971668-583b-4135-8473-70e928502555')
            }
        }
    }

// build environment + bindings
wrappers {
        sshAgent('adop-jenkins-master')
        credentialsBinding {
            usernamePassword('username', 'password', '07971668-583b-4135-8473-70e928502555')
        }
    }

// build- execute shell

steps {
        shell('''ls -la
                ansible-playbook -i hosts master.yml -u ec2-user -e "image_version=$BUILD_NUMBER username=$username password=$password"''')
    }

// post build actions
publishers {
        downstream('act5', 'SUCCESS')
    }
}

freeStyleJob("act5") {

  // source code management  
    scm {
        git {
            remote {
                url('http://13.57.143.226/gitlab/Atienza/SeleniumDTS.git')
                credentials('07971668-583b-4135-8473-70e928502555')
            }
        }
    }

    // build
    steps {
        maven{
            mavenInstallation('ADOP Maven')
            goals('test')
        }
    }
    // post build actions
     publishers {
        downstream('act6', 'SUCCESS')
    }
}

freeStyleJob("act6") {

   // general
    properties {
        copyArtifactPermissionProperty {
        projectNames('act3')
        }
    }  

    // build
    steps {
        copyArtifacts('act3') {
            includePatterns('**/*.war')
            fingerprintArtifacts()
            buildSelector {
                latestSuccessful(true)
            }
        }
            
        nexusArtifactUploader {
            nexusVersion('NEXUS2')
            protocol('http')
            nexusUrl('nexus:8081/nexus')
            groupId('DTSActivity')
            version('${BUILD_NUMBER}')
            repository('releases')
            credentialsId('07971668-583b-4135-8473-70e928502555')
            artifact {
                artifactId('CurrencyConverter')
                type('war')
                file('target/CurrencyConverter.war')
        }
      }
    }

}

buildPipelineView("Sample_Cartridge_Atienza") {
    filterBuildQueue()
    filterExecutors()
    title('Cartridge Pipeline')
    displayedBuilds(3)
    selectedJob('act1')
    alwaysAllowManualTrigger()
    showPipelineParameters()
    refreshFrequency(60)
}
