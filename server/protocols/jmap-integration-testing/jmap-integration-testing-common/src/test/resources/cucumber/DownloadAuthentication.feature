Feature: Download authentication
  As a James user
  I want to access to the download endpoint

  Background:
    Given a domain named "domain.tld"

  Scenario: A known user should access to the download endpoint
    Given a current user with username "username@domain.tld" and password "secret"
    When asking the download endpoint
    Then the user should be authorized

  Scenario: An known user should access to the download endpoint
    Given an unknown user with username "unknown@domain.tld" and password "secret"
    When asking the download endpoint
    Then the user should not be authorized

  Scenario: A known user should not access to the download endpoint without a blobId
    Given a current user with username "username@domain.tld" and password "secret"
    When asking the download endpoint without blobId parameter
    Then the user should receive a bad request response
