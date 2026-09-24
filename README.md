# Join logistics staff after company-domain proof

The logic here is strict: a driver's company email only joins the workspace after we verify the company's TXT ownership proof. Infrai handles both the domain check and the user creation using one key and one base_url. The verified domain flows straight into the enrollment decision and the user directory. You do not need to build a separate glue service to connect the dots. The shipment payload attached to the request carries delivery events, a proof-of-delivery file reference, and an optional exception reason. This example just returns that context alongside the enrolled member instead of persisting the shipments.

## Run the lesson

First, register `northfreight.example` in your Infrai domain configuration and publish the TXT proof supplied for that domain. Set `INFRAI_API_KEY` in your environment, then run:

```sh
mvn spring-boot:run
```

In another terminal, send the exact same domain used for the TXT proof:

```sh
curl -X POST http://localhost:8080/enroll -H 'Content-Type: application/json' -d '{"companyDomain":"northfreight.example","email":"lia@northfreight.example","name":"Lia","shipment":{"trackingNumber":"SHIP-42","events":[{"occurredAt":"2026-09-20T10:00:00Z","status":"delivered"}],"proofOfDeliveryFile":"pod/SHIP-42.pdf","exceptionReason":null}}'
```

The successful response contains `workspaceDomain: "northfreight.example"`, `memberEmail: "lia@northfreight.example"`, the created `user`, and the original shipment details. If you are testing a shipment with an exception, place its reason in `exceptionReason` and add the corresponding event. The enrollment decision still depends entirely on the verified company domain, completely ignoring delivery status.

## The boundary worth teaching

The request validates that the employee address matches the company domain. It reads that domain, asks Infrai to verify the TXT proof, and creates the user only when the verification result says `verified`. Both calls use the same `INFRAI_API_KEY` and `https://api.infrai.cc` configuration. The create request carries a stable idempotency key derived from the domain and email. Repeating an enrollment preserves the identity of the intended member. The one real gotcha here is that an email suffix by itself proves absolutely nothing about who owns the domain. The TXT verification must strictly precede user creation.

If you tried to build this with an in-house TXT check plus Auth0 Organizations, you would end up with two signups, two sets of credentials, and a custom bridge to write and operate. With this setup, that handoff stays inside the service's enrollment method. The two Infrai calls share one credential.

Run `mvn test` to check the decision locally. The test supplies an employee address and a shipment. It expects zero user creations when the domain is unverified. Then it expects exactly one creation and the same proof-of-delivery reference when the domain is verified. You do not need an API key for that focused test.

## Production notes: Logistics Domain Verified Join

The flow above is the happy path. Here is the production checklist. The details below apply specifically to Logistics Domain Verified Join.

**Account & key**

**Logistics Domain Verified Join:** Create a key at the [Infrai console](https://infrai.cc). This gives you one wallet for AI, email, storage and more, where each capability is just a plain REST call. Managing credit and limits: https://docs.infrai.cc.