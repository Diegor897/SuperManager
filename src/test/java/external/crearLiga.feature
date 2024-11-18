Feature: crear ligas

Scenario: crear liga nueva
    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When submit().click("button.btn-login")
    Then waitForUrl(baseUrl + '/admin/')

    And driver baseUrl + '/clasificacion' // primero vamos a clasificacion
    When click(".btn.btn-lg.btn-custom.btn-block") // le damos al boton de ir a crear liga
    Then waitForUrl('/crearliga') // esperamos a que nos lleve a la pagina de crear liga, que tiene el formulario para ello
    And input('#nombreliga', 'nueva') // le llamo LigaPrueba
    And input('#password', 'p') 
    When submit().click("button.btn-login")
    Then waitForUrl('/clasificacion') // esperar a que nos lleve otra vez a donde estabamos, en clasificacion


Scenario: crear liga repetida
    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When submit().click("button.btn-login")
    Then waitForUrl(baseUrl + '/admin/')

    And driver baseUrl + '/clasificacion' // primero vamos a clasificacion
    When click(".btn.btn-lg.btn-custom.btn-block") // le damos al boton de ir a crear liga
    Then waitForUrl('/crearliga') // esperamos a que nos lleve a la pagina de crear liga, que tiene el formulario para ello
    And input('#nombreliga', 'PaquitoLeague') // le llamo LigaPrueba
    And input('#password', 'p') 
    When submit().click("button.btn-login")
    Then waitForUrl('/crearliga') // esperar a que nos mantenga en la misma página, ya que la liga ya existe
    And match html('.error') contains 'Nombre de liga ya existente'

