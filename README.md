# Sistema Gestionale Autosalone

> Sistema RESTful per la digitalizzazione della gestione operativa, commerciale e contabile di un autosalone, sviluppato in Java con architettura Jakarta EE.

## 🎯 Obiettivo del Progetto
Il progetto ha l'obiettivo di realizzare un sistema volto a digitalizzare la gestione operativa e commerciale di un *autosalone*. Fornisce ai proprietari un centro di controllo attraverso cui amministrare l’inventario di veicoli, generare documenti di vendita (da bozza di preventivo a contratto finale) e monitorare i profitti aziendali tracciando entrate e uscite. Allo stesso tempo, offre al cliente un’area dedicata per la consultazione del proprio storico acquisti e delle pratiche in corso, incluse le scadenze relative alle autovetture in modo da agevolare il tracciamento della manutenzione del veicolo.

## 📝 Descrizione del Sistema
Il sistema comprende due tipologie di utente con permessi e interfacce dedicate:
- **Proprietario (`Owner`)**
- **Cliente (`Customer`)**

Il **proprietario** è responsabile di aggiungere e modificare veicoli, documenti commerciali (preventivi e contratti) e utenti (clienti e altri proprietari). Inoltre, gestisce le informazioni collegate a queste risorse principali, come le spese e le scadenze associate a un'autovettura, o gli accessori applicati a un documento di vendita per uno specifico cliente. Le spese relative al veicolo rappresentano costi aziendali interni e non visibili al cliente. Consentono di tracciare le uscite e forniscono un parametro per definire il prezzo di vendita finale dell'auto.

Il **catalogo degli accessori** permette di consultare, modificare o rimuovere gli articoli applicabili ai documenti di vendita. Questi possono essere singole unità con un prezzo base, oppure raggruppati in pacchetti (che a loro volta possono contenere altri pacchetti). In quest'ultimo caso, il prezzo è calcolato tramite la somma dei prezzi base degli articoli contenuti all'interno.

Il ciclo di vita di veicoli, preventivi e contratti è gestito tramite **stati** che precludono operazioni di modifica nelle fasi successive alla loro approvazione, garantendo la coerenza contabile e la validità commerciale degli accordi stipulati.

Le **scadenze**, impostate dai proprietari, possono avere una cadenza ricorrente (es. ogni due anni) o essere eventi una tantum. Ogni **cliente** può consultare i propri preventivi, contratti e le auto associate, contrassegnandone il completamento e innescando automaticamente la generazione dell'evento successivo (es. la prossima revisione).

---

## 🛠️ Tecnologie Principali
- **Core:** Java 21, Jakarta EE 10
- **Database & ORM:** PostgreSQL, Hibernate / JPA
- **API & Architettura:** JAX-RS (RESTful APIs), CDI
- **Sicurezza:** JWT (JSON Web Tokens), BCrypt
- **Infrastruttura:** WildFly Application Server, Docker, Maven
- **Testing:** JUnit 5, Mockito

---

## ⚙️ Come avviare il progetto

Il progetto utilizza **Docker Compose** per orchestrare i database necessari all'applicazione e all'ambiente di testing.

1. Creare il file delle variabili d'ambiente partendo dal template:
   ```bash
   cp .env.template .env
   ```
2. Avviare i container dei database (Principale e Test):
   ```bash
   docker compose up -d
   ```
3. Avviare l'Application Server WildFly ed eseguire il deploy (tramite Maven):
   ```bash
   mvn clean wildfly:run
   ```

---

## 🧩 Casi d'Uso (Use Cases)

Di seguito sono riportati i template espandibili dei principali flussi operativi del sistema. *Clicca sulle singole categorie per espandere i dettagli.*

<details>
<summary><strong>👤 1. Gestione Utenze e Autenticazione</strong></summary>

<br>

**UC-01: Aggiunta Utente**
* **Attore:** Owner
* **Descrizione:** L'Owner registra nel sistema i dati anagrafici e di contatto di un nuovo utente specificandone il ruolo (Cliente o nuovo Proprietario/Socio).
* **Flusso principale:**
  1. L'Owner compila il form con i dati dell'utente e seleziona il ruolo desiderato.
  2. Il sistema valida i dati inseriti.
  3. Il sistema salva la nuova entità nel database.
* **Post-condizioni:** L'utente è salvato nel database senza password di accesso (account inattivo).

**UC-02: Invio Invito di Registrazione**
* **Attore:** Owner
* **Descrizione:** L'Owner invita un utente precedentemente inserito ad accedere al portale tramite la generazione di un Token temporaneo e l'invio di una email.

**UC-03: Completamento Registrazione**
* **Attore:** User (Non Autenticato)
* **Descrizione:** L'utente utilizza il link ricevuto via email per impostare la propria password (tramite hash BCrypt) e attivare l'account.

**UC-04: Login**
* **Attore:** User (Non Autenticato)
* **Descrizione:** L'utente accede al sistema inserendo le proprie credenziali. In caso di successo, il sistema restituisce un token JWT firmato crittograficamente per mantenere la sessione _stateless_.

</details>

<details>
<summary><strong>🚗 2. Veicoli, Spese e Scadenze</strong></summary>

<br>

**UC-05: Aggiunta Veicolo**
* **Attore:** Owner
* **Descrizione:** L'Owner inserisce i dati tecnici e commerciali di un veicolo nuovo o usato, incluso il prezzo di vendita e opzionalmente il costo aziendale di acquisto.
* **Post-condizioni:** Il veicolo è disponibile in stato `AVAILABLE`. Se compilata, viene generata una Transazione in uscita (`OUT`) per tracciare il costo di acquisto.

**UC-06: Aggiunta Spesa su Veicolo**
* **Attore:** Owner
* **Descrizione:** L'Owner registra una spesa sostenuta per un veicolo (es. riparazione, lavaggio). Genera una Transazione `OUT` invisibile al cliente.

**UC-07: Generazione Scadenze Revisione**
* **Attori:** Owner / System
* **Descrizione:** Il sistema calcola e imposta la scadenza ministeriale per la revisione. Avviene in automatico per le auto nuove (prima scadenza a 4 anni), o su input manuale dell'Owner per le usate.

**UC-08: Completamento Scadenza**
* **Attori:** Customer / Owner
* **Descrizione:** L'utente contrassegna come eseguito un evento. Se la scadenza prevede una ricorrenza (es. annuale), il sistema calcola la prossima data e genera automaticamente un nuovo evento pendente proiettato nel futuro.

</details>

<details>
<summary><strong>📦 3. Gestione Catalogo</strong></summary>

<br>

**UC-09: Aggiunta Elemento Catalogo**
* **Attore:** Owner
* **Descrizione:** Aggiunta di un nuovo accessorio singolo o un pacchetto. Per i pacchetti, il sistema sfrutta il _Composite Pattern_ per calcolare automaticamente il prezzo totale sommando i prezzi base dei suoi componenti.

**UC-10: Eliminazione Elemento Catalogo**
* **Attore:** Owner
* **Descrizione:** Rimozione fisica o archiviazione logica. Se l'articolo è già utilizzato da documenti di vendita, la rimozione fisica è bloccata per mantenere l'integrità referenziale, procedendo invece con un _Soft Delete_ (archiviazione).

</details>

<details>
<summary><strong>📄 4. Documenti di Vendita (Preventivi e Contratti)</strong></summary>

<br>

**UC-11: Creazione Preventivo (Bozza)**
* **Attore:** Owner
* **Descrizione:** Avvio di una proposta commerciale. Il sistema crea una `Quotation` in stato `DRAFT` e congela i prezzi degli articoli annessi dal catalogo trasformandoli in `AppliedItem`.

**UC-12: Genera Contratto da Preventivo**
* **Attore:** Owner
* **Descrizione:** Generazione di una bozza di contratto a partire da un preventivo emesso. I prezzi storici pattuiri vengono mantenuti inalterati.

**UC-13: Conferma Contratto**
* **Attore:** Owner
* **Descrizione:** Formalizzazione del contratto. Il veicolo passa in stato `RESERVED`. Il sistema crea uno *snapshot* (copia immutabile) del profilo attuale del cliente per preservarne l'integrità legale.

**UC-14: Completamento Contratto**
* **Attore:** Owner
* **Descrizione:** A seguito della consegna del veicolo e della verifica del saldo totale pari a zero, il contratto passa a `COMPLETED` e il veicolo a `SOLD`.

**UC-15: Scadenza Automatica Preventivi**
* **Attore:** System (Scheduled Task)
* **Descrizione:** Sfruttando le _Concurrency API_ (Scheduler), il sistema controlla quotidianamente in background i preventivi emessi. Se la data di scadenza è superata, passano a `EXPIRED` e l'eventuale veicolo bloccato viene liberato (`AVAILABLE`).

</details>

<details>
<summary><strong>💶 5. Transazioni Finanziarie</strong></summary>

<br>

**UC-16: Gestione Transazioni Contratto (Acconti e Rimborsi)**
* **Attore:** Owner
* **Descrizione:** Registrazione di flussi in entrata (IN) o uscita (OUT). Il sistema valida matematicamente che un acconto non superi il debito residuo e che un rimborso non superi il totale già incassato.

</details>
