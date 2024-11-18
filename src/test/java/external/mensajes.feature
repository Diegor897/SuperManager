Feature: reportar mensaje

Scenario: escribir mensaje
    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When submit().click("button.btn-login")
    Then waitForUrl(baseUrl + '/admin/')
    And driver baseUrl + '/actualidad/'
    When waitFor(".card-title[href='/foro/4']").click()
    Then waitForUrl(baseUrl + '/foro/4')
    And input('#message', 'quiero reportar este mensaje') //
    When click("button#sendmsg.Foroboton") //

Scenario: reportar mensaje

    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When submit().click("button.btn-login")
    And driver baseUrl + '/actualidad/'
    When waitFor(".card-title[href='/foro/4']").click()
    Then waitForUrl(baseUrl + '/foro/4')
    When click("button.Foroboton") //

Scenario: ver mensaje reportado

    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When submit().click("button.btn-login")
    Then waitForUrl(baseUrl + '/admin/')

    And waitFor("button.btn.btn-lg.btn-custom.btn-block[name='formType'][value='mensajesReportados']") // Espera hasta que el botón específico esté visible
    When click("button.btn.btn-lg.btn-custom.btn-block[name='formType'][value='mensajesReportados']") // Pulsa en el botón específico
    Then waitForUrl('/admin/mensajesReportados') // Espera a que la URL cambie
