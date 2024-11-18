Feature:

Scenario:
Feature: login en servidor

#
#  Este test funciona, pero no es de buena educación martillear una API externa
#

  Scenario: login malo en plantilla
    Given driver baseUrl + '/user/2'
    And input('#username', 'dummy')
    And input('#password', 'world')
    When submit().click("button.btn-login") // cambiado
    Then match html('.error') contains 'Error en nombre de usuario o contraseña'

  @login_b
  Scenario: login correcto como b
    Given driver baseUrl + '/login'
    And input('#username', 'b')
    And input('#password', 'aa')
    When submit().click("button.btn-login")
    Then waitForUrl(baseUrl + '/user/3')

  @login_a
  Scenario: login correcto como a
    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When submit().click("button.btn-login")
    Then waitForUrl(baseUrl + '/admin/')

  Scenario: logout after login
    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When submit().click("button.btn-login")
    Then waitForUrl(baseUrl + '/admin')
    When submit().click("{button}logout")
    Then waitForUrl(baseUrl + '/login')