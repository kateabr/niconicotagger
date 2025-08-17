export interface EventScheduleRequest {
  clientType: string;
  useCached: boolean;
  eventScopeDays: number | null;
}
