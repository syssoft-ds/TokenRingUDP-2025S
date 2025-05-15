
Da ich mit einer Grippe im Bett lag, konnte ich mich leider nicht mit anderen zusammentun,
um gemeinsam einen Tokenring aufzubauen. Also beschreibe ich die erste Aufgabe anstatt der Zweiten.

Anfangs hatte ich Schwierigkeiten beim Kompilieren des Projekts, da ich den lib-Ordner mit 
den notwendigen .jar-Dateien nicht explizit im Classpath angegeben hatte. Nachdem ich das 
korrigiert hatte, ließ sich das Projekt erfolgreich kompilieren und starten.

Ich habe drei Terminalfenster geöffnet und darin das Java-Programm jeweils gestartet. In den 
zweiten und dritten Fenstern habe ich zusätzlich die IP-Adresse und den Port des ersten Knotens 
angegeben, um den Ring zu schließen. Die Kommunikation lief daraufhin 
fehlerfrei.

Beim Einsatz von Wireshark hatte ich zunächst Schwierigkeiten, die verschickten UDP-Pakete 
zu finden. Obwohl ich mehrere Filter ausprobierte, blieb die Anzeige zunächst leer. Nach etwa 
einer halben Stunde stellte ich fest, dass ich im falschen Netzwerk-Interface suchte – nämlich 
im WLAN anstatt im lokalen Loopback-Interface. Nachdem ich das Interface gewechselt hatte, 
konnte ich die gesuchten Pakete sofort beobachten.

Als Filterfunktion habe ich (ip.addr == <im Terminal angezeigte IP-Adresse>) and udp verwendet, 
was sehr gut funktionierte und mir direkt die relevanten UDP-Pakete des Tokenrings angezeigt hat.