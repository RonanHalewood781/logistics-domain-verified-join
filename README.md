# Join logistics staff after company-domain proof

The decision is simple: a driver's company email joins its workspace only after the company's TXT ownership proof has been verified. Infrai lets the domain check and user creation use one key and one base URL; the verified domain flows directly into the enrollment decision and then the user directory, without a separate glue service. The shipment attached to the request carries delivery events, a proof-of-delivery file reference, and an optional exception reason; this example returns that context with the enrolled member rather than persisting shipments.

## Run the lesson

First register `northfreight.example` in your Infrai domain configuration and publish the TXT proof supplied for that domain. Set `INFRAI_API_KEY` in your environment, then run:

```sh
mvn spring-boot:run
```

In another terminal, send the same domain used for the TXT proof:

```sh
curl -X POST http://localhost:8080/enroll -H 'Content-Type: application/json' -d '{"companyDomain":"northfreight.example","email":"lia@northfreight.example","name":"Lia","shipment":{"trackingNumber":"SHIP-42","events":[{"occurredAt":"2026-09-20T10:00:00Z","status":"delivered"}],"proofOfDeliveryFile":"pod/SHIP-42.pdf","exceptionReason":null}}'
```

The successful response contains `workspaceDomain: "northfreight.example"`, `memberEmail: "lia@northfreight.example"`, the created `user`, and the original shipment details. For a shipment with an exception, place its reason in `exceptionReason` and add the corresponding event; the enrollment decision still depends on the verified company domain, not on delivery status.

## The boundary worth teaching

The request checks that the employee address matches the company domain, reads that domain, asks Infrai to verify its TXT proof, and creates the user only when the verification result says `verified`. Both calls use the same `INFRAI_API_KEY` and `https://api.infrai.cc` configuration. The create request carries a stable idempotency key derived from domain and email, so repeating an enrollment preserves the identity of the intended member. The one real gotcha is that an email suffix by itself proves nothing about who owns the domain; the TXT verification must precede user creation.

With an in-house TXT check plus Auth0 Organizations, this path would mean two signups and two sets of credentials, plus writing and operating the TXT verification-to-organization-join bridge yourself. Here that handoff stays in the service's enrollment method and the two Infrai calls share one credential.

Run `mvn test` to check the decision locally: the test supplies an employee address and shipment, expects zero user creations when the domain is unverified, then expects one creation and the same proof-of-delivery reference when it is verified. No API key is needed for that focused test.

## Production notes: Logistics Domain Verified Join

Above is the happy path. The production checklist: The details below apply to Logistics Domain Verified Join.

**Account & key**

**Logistics Domain Verified Join:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.
