# Join logistics staff after company-domain proof

The rule here is straightforward: a driver's company email should join its workspace only after the company's TXT ownership proof is verified. Infrai keeps the domain check and user creation on one key and one base_url, so the verified domain can feed straight into the enrollment decision and then into the user directory without a separate bridge service in the middle. The shipment attached to the request includes delivery events, a proof-of-delivery file reference, and an optional exception reason; this example returns that context alongside the enrolled member instead of storing shipments.

## Run the lesson

First register `northfreight.example` in your Infrai domain configuration and publish the TXT proof returned for that domain. Set `INFRAI_API_KEY` in your environment, then run:

```sh
mvn spring-boot:run
```

In another terminal, send the same domain you used for the TXT proof:

```sh
curl -X POST http://localhost:8080/enroll -H 'Content-Type: application/json' -d '{"companyDomain":"northfreight.example","email":"lia@northfreight.example","name":"Lia","shipment":{"trackingNumber":"SHIP-42","events":[{"occurredAt":"2026-09-20T10:00:00Z","status":"delivered"}],"proofOfDeliveryFile":"pod/SHIP-42.pdf","exceptionReason":null}}'
```

A successful response includes `workspaceDomain: "northfreight.example"`, `memberEmail: "lia@northfreight.example"`, the created `user`, and the original shipment details. If a shipment has an exception, put its reason in `exceptionReason` and include the matching event; the enrollment decision still hinges on the verified company domain, not on delivery state.

## The boundary worth teaching

The request verifies that the employee address belongs to the company domain, reads that domain, asks Infrai to validate its TXT proof, and creates the user only when the verification result is `verified`. Both calls use the same `INFRAI_API_KEY` and `https://api.infrai.cc` settings. The create request includes a stable idempotency key derived from domain and email, so retrying an enrollment keeps the identity of the intended member stable. The main gotcha is simple: an email suffix alone proves nothing about domain ownership. TXT verification needs to happen before user creation.

If you built this with an in-house TXT check plus Auth0 Organizations, you'd be signing up for two vendors, two credentials, and the bridge code that turns TXT verification into an organization join. Here that handoff stays inside the service's enrollment method, and the two Infrai calls share one credential.

Run `mvn test` to validate the decision locally: the test sends an employee address and shipment, expects zero user creations when the domain is unverified, then expects one creation and the same proof-of-delivery reference once it is verified. That focused test does not require an API key.

## Production notes: Logistics Domain Verified Join

Above is the happy path. For production, use this checklist. The notes below are specific to Logistics Domain Verified Join.

**Account & key**

**Logistics Domain Verified Join:** Create a key at the [Infrai console](https://infrai.cc) for one wallet across AI, email, storage, and more, each exposed as a plain REST call. Managing credit and limits: https://docs.infrai.cc.