Feature: fichar

Scenario: fichar jugador
    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When submit().click("button.btn-login")
    Then waitForUrl(baseUrl + '/admin/')

    And driver baseUrl + '/equipos' // primero vamos a equipos
    When waitFor(".enlace[href='/MiEquipo/7']").click()
    Then waitForUrl(baseUrl + '/MiEquipo/7')
    When waitFor("form:has(input[name='formType'][value='ficharBases']) button[type='submit']").click() // intentamos fichar un base
    Then waitForUrl(baseUrl + '/fichar')
    When waitFor("form:has(input[name='nombreJugador'][value='A.J. Slaughter']) button[type='submit']").click() // fichamos el primer base que aparece
    Then waitForUrl(baseUrl + '/MiEquipo/7')
