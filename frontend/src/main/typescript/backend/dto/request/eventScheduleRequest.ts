import { ClientType } from "@/backend/dto/enumeration";

export interface EventScheduleRequest {
  clientType: ClientType;
  useCached: boolean;
}
