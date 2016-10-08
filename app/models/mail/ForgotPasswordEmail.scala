package models.mail

object ForgotPasswordEmail {
  val subject = "Recuperar acesso - Troca de senha"
  def content(url: String, host: String) =
    s"""
     <html>
        <head>
          <meta charset="utf-8">
          <style>
            body {
              font-family: "verdana"
            }
          </style>
        </head>
        <body>
              <h2>Recuperação de senha</h2>
              <hr />
              <p>Você requisitou o procedimento de recuperação de acesso a sua conta.</p>
              <p>Clique no link abaixo para ser redirecionado para que possa trocar sua senha.</p>
              <a href="https://${host+"/"+url}">https://${host+"/"+url}</a>
              <br />
              <p>Caso o link não esteja disponível no seu cliente de email, copie e cole
              o endereço acima na sua barra de endereços.</p>
              <p><strong>Atenção: O link acima só sera válido por 3 (três) horas.
              Após isso você terá de requisitar novamente.</strong></p>
              <p>Se você não requisitou esse e-mail, pode ignorará-lo, mas fique atento.</p>
              <p>Atenciosamente,<br /> SAEB Admin.</p>
        <body>
     </html>
    """

  def apply(url: String, host: String, to: String): DefaultEmail =
    new DefaultEmail(subject, to, content(url, host))
}

