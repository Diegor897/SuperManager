
// envio de mensajes con AJAX
let b = document.getElementById("sendmsg");
if (b){
    b.onclick = (e) => {
        e.preventDefault();
        go(b.parentNode.action, 'POST', {
                message: document.getElementById("message").value
            })
            .then(d => console.log("La d tiene el valor: ", d))
            .catch(e => console.log("sad", e))
    }
    
    // cómo pintar 1 mensaje (devuelve html que se puede insertar en un div)
    function renderMsg(msg) {
        console.log("rendering: ", msg);
    
        const fechaHora = new Date(msg.sent);
     
        const dia = fechaHora.getDate();
        const mes = fechaHora.getMonth() + 1; 
     
        const diaFormateado = dia < 10 ? `0${dia}` : dia;
        const mesFormateado = mes < 10 ? `0${mes}` : mes;
     
        const hora = fechaHora.getHours();
        const minutos = fechaHora.getMinutes();
     
        const horaFormateada = hora < 10 ? `0${hora}` : hora;
        const minutosFormateados = minutos < 10 ? `0${minutos}` : minutos;
     
        const fechaFormateada = `${diaFormateado}/${mesFormateado}<br>${horaFormateada}:${minutosFormateados}`; 
        
        const userId = msg.fromId; // Ajusta esto según la estructura real de msg.from
        const username = msg.from; // Ajusta esto según la estructura real de msg.from

        // Incorporamos el botón en el HTML del mensaje si es necesario       

        return `
            <div class="infoMensaje">
                <p class="textoFrom">
                    <a class="enlace" href="/perfilPublico/${userId}" >${username}</a><br>
                </p>
                    <p class="textoFecha">${fechaFormateada}:</p>
                    <p class="textoMensaje">${msg.text}</p> <!-- Mensaje separado en una clase diferente -->
                    <button class="Foroboton" onclick="reportMessage(${msg.id})">Reportar</button>
                </div>`;
    }
    
    let idLiga = window.location.pathname.split("/").pop();
    
    function reportMessage(msgId) {
        let url = `${config.rootUrl}/foro/${idLiga}`
        go(url, "PUT", {
            idMensaje: msgId
        }).then(() => {
            // Encuentra el toast en el DOM
            const toastEl = document.getElementById("reportToast");
    
            // // Usa Bootstrap para mostrar el toast
            const toast = new bootstrap.Toast(toastEl);
            toast.show(); // Muestra el toast
    
        }).catch(error => {
            console.error("Error al mostrar la notificacion:", error);
        });
    }
    
    // pinta mensajes viejos al cargarse, via AJAX
    let messageDiv = document.getElementById("mensajes");
    let foroUrl = `${config.rootUrl}/mensajes/${idLiga}`;
    go(foroUrl, "GET").then(ms => {
       console.log("Cargando mensajes antiguios", ms);
       ms.forEach(m => messageDiv.insertAdjacentHTML("beforeend", renderMsg(m)));})
    
    // y aquí pinta mensajes según van llegando
    if (ws.receive) {
       const oldFn = ws.receive; // guarda referencia a manejador anterior
        ws.receive = (m) => {
           oldFn(m); // llama al manejador anterior
           messageDiv.insertAdjacentHTML("beforeend", renderMsg(m));
        }
    }
}

// avanzarJornada con AJAX
let avanzarJornada = document.getElementById("avanzarJornada");
let adminUrl = `${config.rootUrl}/admin/`;
console.log("AvanzarJornada es ", avanzarJornada);
if (avanzarJornada) {
 avanzarJornada.onclick = (e) => {
     avanzarJornada.disabled = true
     e.preventDefault();
     go(avanzarJornada.parentNode.action, 'POST')
         .then(d =>{ 
             console.log("happy", d)
             avanzarJornada.disabled = false
             window.location.href = adminUrl;
         })
         .catch(e => {
             console.log("sad", e)
             avanzarJornada.disabled = false
         })
 }
}

// click en botones de "usar como foto de perfil"
document.querySelectorAll(".perfilable").forEach(o => {
    o.onclick = e => {
        e.preventDefault();
        let url = o.parentNode.action;
        let img = o.parentNode.parentNode.querySelector("img");
        postImage(img, url, "photo").then(() => {
            let cacheBuster = "?" + new Date().getTime();
            document.querySelector("a.nav-link>img.iwthumb").src = url + cacheBuster;
        });
}});

// refresca previsualizacion cuando cambias imagen
let avatar = document.querySelector("#f_avatar")
if (avatar) {
    avatar.onchange = e => {
        let img = document.querySelector("#avatar");
        let fileInput = document.querySelector("#f_avatar");
        console.log(img, fileInput);
        readImageFileData(fileInput.files[0], img);
    };
    console.log("El botón objetivo es ", document.querySelector("#postAvatar"));
}

// click en boton de enviar avatar
let postAvatar = document.querySelector("#postAvatar")
if (postAvatar) {
    postAvatar.onclick = e => {
        e.preventDefault();
    
        let url = document.querySelector("#postAvatar").parentNode.action;
        let img = document.querySelector("#avatar");
        let file = document.querySelector("#f_avatar");
        postImage(img, url, "photo").then(() => {
            let cacheBuster = "?" + new Date().getTime();
            document.querySelector("a.nav-link>img.iwthumb").src = url + cacheBuster;
        });
    };
}



// // ver https://openlibrary.org/dev/docs/api/books
// // no requieren "api key", pero necesitas 1 consulta adicional por autor
// function fetchBookData(isbn, targetImg) {
//    go(`https://openlibrary.org/isbn/${isbn}.json`, "GET", {}, {}).then(bookInfo => {
//        authorLookups = bookInfo.authors.map(a =>
//            go(`https://openlibrary.org${a.key}.json`, "GET", {}, {}));
//        console.log(`title: ${bookInfo.title}`);
//        //targetImg.src = `https://covers.openlibrary.org/b/id/${bookInfo.covers[0]}-M.jpg`;
//        readImageUrlData(`https://covers.openlibrary.org/b/id/${bookInfo.covers[0]}-M.jpg`, targetImg);
//        Promise.all(authorLookups).then(authorInfos => {
//            for (let a of authorInfos) {
//                console.log(`Author: ${a.name}`);
//            }
//        });
//    })
// }

// // ver https://www.omdbapi.com/
// // requieren API key, pero se puede conseguir de forma gratuita
// // (no uses mucho la que hay ahí abajo, por favor!)
// function fetchMovieData(imdb, targetImg) {
//    go(`http://www.omdbapi.com/?i=${imdb}&apikey=174a19fd`, "GET", {}, {}).then(movieInfo => {
//        console.log(`title: ${movieInfo.Title}`);
//        // targetImg.src = movieInfo.Poster;
//        readImageUrlData(movieInfo.Poster, targetImg)
//    })
// }

// // click en boton de cargar datos libro
// document.querySelector("#fetchBook").onclick = e => {
//    let isbn = document.querySelector("#isbn").value;
//    console.log("fetching ", isbn);
//    fetchBookData(isbn, document.querySelector("#portada"));
// };
// // click en boton de cargar datos peli
// document.querySelector("#fetchMovie").onclick = e => {
//    let imdb = document.querySelector("#imdb").value;
//    console.log("fetching ", imdb);
//    fetchMovieData(imdb, document.querySelector("#poster"));
// };


    


