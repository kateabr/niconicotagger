import { ClientType } from "@/backend/dto/enumeration";

export interface GetReleaseEventRequest {
  eventName: string;
  clientType: ClientType;
}
