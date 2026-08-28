with open("src/main/java/com/eneik/epidemiology/security/SecurityConfig.java", "r") as f:
    text = f.read()

text = text.replace(".anyRequest().authenticated()", ".requestMatchers(\"/api/v1/dossier/**\", \"/api/v1/documents/**\").permitAll()\n                .anyRequest().authenticated()")

with open("src/main/java/com/eneik/epidemiology/security/SecurityConfig.java", "w") as f:
    f.write(text)
