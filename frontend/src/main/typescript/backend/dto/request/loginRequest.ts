import { ClientType } from "@/backend/dto/enumeration";

export interface LoginRequest {
  userName: string;
  password: string;
  clientType: ClientType;
}
