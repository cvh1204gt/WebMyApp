/*==================== SHOW NAVBAR ====================*/
const showMenu = (headerToggle, navbarId) =>{
    const toggleBtn = document.getElementById(headerToggle),
    nav = document.getElementById(navbarId)
    
    // Validate that variables exist
    if(headerToggle && navbarId){
        toggleBtn.addEventListener('click', ()=>{
            // We add the show-menu class to the div tag with the nav__menu class
            nav.classList.toggle('show-menu')
            // change icon
            toggleBtn.classList.toggle('bx-x')
        })
    }
}
showMenu('header-toggle','navbar')

/*==================== LINK ACTIVE ====================*/
const linkColor = document.querySelectorAll('.nav__link')

function colorLink(){
    linkColor.forEach(l => l.classList.remove('active'))
    this.classList.add('active')
}

linkColor.forEach(l => l.addEventListener('click', colorLink))

/*==================== EYE ICON ====================*/
const eyeIcon = document.querySelectorAll(".fa-eye");
    
    eyeIcon.forEach(icon => {
      icon.addEventListener("click", () => {
        const input = icon.previousElementSibling;

        icon.classList.toggle("fa-eye-slash");
        
        if(icon.classList.contains("fa-eye-slash")) {
          input.type = "text";
        } else {
          input.type = "password";
        }

      });
    });
