import { ReleaseEventCategory } from "@/backend/dto/enumeration";
import { VocaDbTag } from "@/backend/dto/lowerLevelStruct";

export interface ReleaseEventWithLinkedTagResponse {
  id: number;
  date: string;
  endDate: string;
  name: string;
  category: ReleaseEventCategory;
  vocaDbTags: VocaDbTag[];
}
