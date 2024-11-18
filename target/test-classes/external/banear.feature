Feature: banear

Scenario: banear usuario

    # Primero me logueo como admin (a)
    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When submit().click("button.btn-login")
    Then waitForUrl(baseUrl + '/admin/') 

   
    When click(".btn.btn-lg.btn-custom.btn-block") // le damos al boton de ver usuarios
    Then waitForUrl('/admin/usuarios?formType=listadoUsuarios') // esperamos a que nos lleve a la pagina de ver usuarios
    
    When waitFor("form:has(input[name='idUsuario'][value='3']) button[type='submit']").click() // especificar el usuario al que se quiere banear
    When submit().click("button[name='formType'][value='banear usuario']") // clickar en el boton de banear
    Then waitForUrl('/admin/banearUsuario') 

Scenario: Entramos como b (baneado)
    Given driver baseUrl + '/login'
    And input('#username', 'b')
    And input('#password', 'aa')
    When submit().click("button.btn-login")
    Then match html('.error') contains 'Error en nombre de usuario o contraseña' // nos tiene que saltar este error





    


