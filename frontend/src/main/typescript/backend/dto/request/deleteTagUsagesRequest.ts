import { ClientType, QueryConsoleApiType } from "@/backend/dto/enumeration";
import { VocaDbTag } from "@/backend/dto/lowerLevelStruct";

export interface DeleteTagUsagesRequest {
  apiType: QueryConsoleApiType;
  entryId: number;
  tags: VocaDbTag[];
  clientType: ClientType;
}
