# Authentication and Credentials

**Language:** English | [简体中文](auth-and-sign.zh-CN.md)

Station OpenAPI SDK supports Access Token authentication and AppKey/SecretKey signature authentication. Each
`StationOpenApiClient` must use exactly one mode.

## 1. Obtain Credentials from Technical Support

Obtain the target environment's HTTP (S) endpoint and credentials from technical support, and confirm the permitted
operations. The endpoint and credentials must belong to the same environment.

## 2. Choose an Authentication Mode

| Mode         | Required configuration | Authentication data sent by the SDK                            |
|--------------|------------------------|----------------------------------------------------------------|
| Access Token | `accessToken`          | HTTP header `X-Access-Token`                                   |
| Signature    | `appKey`, `secretKey`  | `appKey`, `timestamp`, `nonce`, and `sign` in the request body |

Use the mode for which technical support supplied credentials. The Sample enables the Token profile by default and can
be switched to the signature profile.

In signature mode, `secretKey` participates only in local signature generation and is never sent as a request field. The
SDK creates fresh authentication fields for every attempt, including retries.

In Token mode, the SDK sets `X-Access-Token` automatically. Do not add the same header again through an application
interceptor.

## 3. Spring Boot Configuration

See the [Spring Boot Quickstart](quickstart.md) for configuration files and the Client bean.

Token mode must contain only the Access Token. Signature mode must contain only the AppKey and SecretKey. The Sample
rejects mixed credentials during application startup.

## 4. Clock Requirements for Signature Mode

Signatures contain the current timestamp, so the host running the application must keep an accurate system clock. If
Token authentication works but signature authentication repeatedly fails, check the system time first, then confirm that
the AppKey and SecretKey belong to the current platform environment.

Use `client.system().getTimeText()` to verify basic connectivity and authentication.

## 5. Credential Handling Rules

- Never commit real credentials to version control.
- Never expose complete credentials in logs, exceptions, Swagger examples, or API responses.
- Never place `secretKey` in a business request model.
- Never allow external callers to submit platform credentials through your business APIs.
- Contact technical support when credentials expire, become invalid, or need replacement.
