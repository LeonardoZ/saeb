package models.mail

object ReactivateAccountEmail {
  val subject = "Recuperar acesso - Reativação de conta"
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
              <h2>Reativação de conta</h2>
              <hr />
              <p>Foi requisitado para você a reativação da sua conta.</p>
              <p>Clique no link abaixo para ser redirecionado e assim confirmar a reativação.</p>
              <a href="http://${host+"/"+url}">http://${host+"/"+url}</a>
              <br />
              <p>Caso o link não esteja disponível no seu cliente de email, copie e cole
              o endereço acima na sua barra de endereços.</p>
              <p><strong>Atenção: O link acima só sera válido por 24 (vinte e quatro) horas.
              Após isso o link se torna inválido.</strong></p>
              <p>Se você não requisitou esse e-mail, pode ignorará-lo.</p>
              <p>Atenciosamente,<br /> SAEB Admin.</p>
        <body>
     </html>
    """

  def apply(url: String, host: String, to: String): DefaultEmail =
    new DefaultEmail(subject, to, content(url, host))
}

