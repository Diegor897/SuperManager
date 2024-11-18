Feature: crear equipos

Scenario: crear equipo nuevo
    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When submit().click("button.btn-login")
    Then waitForUrl(baseUrl + '/admin/')

    And driver baseUrl + '/equipos' // primero vamos a equipos
    When click(".btn.btn-lg.btn-custom.btn-block") // le damos al boton de ir a crear equipo
    Then waitForUrl('/crearequipo') // esperamos a que nos lleve a la pagina de crear equipo, que tiene el formulario para ello
    And input('#nombreequipo', 'nuevo') //
    When submit().click("button.btn-login")
    Then waitForUrl('/equipos') // esperar a que nos lleve otra vez a donde estabamos, en equipos


Scenario: crear un equipo repetido
    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When submit().click("button.btn-login")
    Then waitForUrl(baseUrl + '/admin/')

    And driver baseUrl + '/equipos' // primero vamos a equipos
    When click(".btn.btn-lg.btn-custom.btn-block") // le damos al boton de ir a crear equipo
    Then waitForUrl('/crearequipo') // esperamos a que nos lleve a la pagina de crear equipo, que tiene el formulario para ello
    And input('#nombreequipo', 'Los canijos') //
    When submit().click("button.btn-login")
    Then waitForUrl('/crearequipo') // esperar a que nos lleve otra vez a donde estabamos, en equipos
    And match html('.error') contains 'Nombre de equipo ya existente'

