package com.ipto.opdefx.payload

object TokenPayload {

  val payload = """
    <soapenv:Envelope xmlns:ser="http://services.soap.interfaces.application.components.opdm.entsoe.eu/" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   			<soapenv:Header>
				<wsse:Security soapenv:mustUnderstand="1"
				xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
				xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
				<wsse:UsernameToken wsu:Id="UsernameToken-1">
				<wsse:Username></wsse:Username>
				<wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText"></wsse:Password>
				</wsse:UsernameToken>
				</wsse:Security>
			</soapenv:Header>
   			<soapenv:Body>
		      <ser:RequestSecurityToken/>
		   </soapenv:Body>
	  </soapenv:Envelope>""".stripMargin

}
