pipelineJob('James-PR') {
  properties {
    githubProjectUrl('https://github.com/aduprat/james-project')
  }
  triggers {
    githubPullRequest {
      orgWhitelist('linagora')
      cron ('* * * * *')
      triggerPhrase('test this please')
      allowMembersOfWhitelistedOrgsAsAdmin()
    }
  }
  definition {
    cpsScm {
      scm {
        git {
          remote {
            url('https://github.com/aduprat/james-project')
            name('origin')
            refspec('+refs/pull/${ghprbPullId}/*:refs/remotes/origin/pr/${ghprbPullId}/*')
          }
          branch('${sha1}')
        }
      }
      lightweight(false)
      scriptPath('ci/JenkinsfileGithubPR')
    }
  }
}
