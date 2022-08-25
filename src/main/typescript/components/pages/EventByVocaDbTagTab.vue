<template>
  <div>
    <error-message
      :alert-code="alertCode"
      :alert-message="alertMessage"
      :this-mode="thisMode"
    />
    <b-row>
      <span class="m-auto col-lg-5">
        <b-input-group inline class="mt-lg-3">
          <template #prepend>
            <b-button
              v-b-toggle="'scope-collapse-' + thisMode"
              variant="primary"
              style="width: 80px"
              :disabled="defaultDisableCondition() || event.id < 0"
              ><font-awesome-icon
                class="mr-sm-1"
                icon="fas fa-angle-down"
              />More</b-button
            >
          </template>
          <b-form-input
            id="tag-form"
            v-model.trim="eventTagName"
            :disabled="defaultDisableCondition()"
            placeholder="Event tag name"
            @keydown.enter.native="loadInitialPage()"
          >
          </b-form-input>
          <template #append>
            <b-button
              v-if="!fetching"
              variant="primary"
              style="width: 80px"
              :disabled="eventTagName === '' || defaultDisableCondition()"
              @click="loadInitialPage()"
              >Load</b-button
            >
            <b-button v-else variant="primary" style="width: 80px" disabled
              ><b-spinner small></b-spinner
            ></b-button>
          </template>
        </b-input-group>
        <b-collapse
          :id="'scope-collapse-' + thisMode"
          v-model="showCollapse"
          class="mt-2"
        >
          <b-row>
            <b-col>
              <template>
                <b-input-group inline>
                  <b-form-input
                    v-model="timeDelta"
                    :disabled="defaultDisableCondition() || !isActiveMode()"
                    number
                    type="range"
                    min="0"
                    :max="timeDeltaMax"
                    @change="filterEntriesIfValidState()"
                  />
                  <template #prepend>
                    <b-input-group-text class="justify-content-center">
                      <b-form-checkbox
                        v-model="timeDeltaEnabled"
                        :disabled="fetching || !isActiveMode()"
                        @change="filterEntriesIfValidState()"
                      />
                      Time delta
                    </b-input-group-text>
                  </template>
                  <template #append>
                    <b-input-group-text class="justify-content-center"
                      >{{ timeDelta }}
                      day(s)
                    </b-input-group-text>
                  </template>
                </b-input-group>
                <b-input-group inline class="mt-2 text-center">
                  <b-checkbox
                    v-model="timeDeltaBefore"
                    :state="getTimeDeltaState()"
                    :disabled="!timeDeltaEnabled"
                    class="col-6"
                    @change="filterEntriesIfValidState()"
                    >before</b-checkbox
                  >
                  <b-checkbox
                    v-model="timeDeltaAfter"
                    :state="getTimeDeltaState()"
                    :disabled="!timeDeltaEnabled"
                    class="col-6"
                    @change="filterEntriesIfValidState()"
                    >after</b-checkbox
                  >
                </b-input-group>
              </template>
            </b-col>
            <b-col>
              <template>
                <b-input-group
                  inline
                  :state="pageStateIsValid"
                  invalid-feedback="Wrong page number"
                >
                  <template #prepend>
                    <b-input-group-text
                      class="justify-content-center"
                      style="width: 80px"
                      >Page:
                    </b-input-group-text>
                  </template>
                  <template>
                    <b-form-input
                      id="page-jump-form"
                      v-model.number="pageToJump"
                      type="number"
                      :disabled="defaultDisableCondition()"
                      aria-describedby="input-live-help input-live-feedback"
                      :state="pageStateIsValid()"
                      @keydown.enter.native="
                        pageStateIsValid() ? loadPage(pageToJump) : null
                      "
                    >
                    </b-form-input>
                  </template>
                  <template #append>
                    <b-button
                      style="width: 80px"
                      :variant="pageStateIsValid() ? 'success' : 'danger'"
                      :disabled="
                        defaultDisableCondition() || !pageStateIsValid()
                      "
                      @click="loadPage(pageToJump)"
                      ><span v-if="pageToJump === page">Refresh</span
                      ><span v-else>Jump</span></b-button
                    >
                  </template>
                </b-input-group>
              </template>
            </b-col>
          </b-row>
        </b-collapse>
      </span>
    </b-row>
    <b-row v-if="eventInfoLoaded() && isActiveMode()">
      <b-row
        class="mt-lg-3 pt-lg-3 pb-lg-3 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
      >
        <b-col>
          <b-row>
            <b-col>
              {{ event.category }}:<br />
              <strong>
                <b-link
                  :href="getVocaDBEventUrl(event.id, event.urlSlug)"
                  target="_blank"
                  >{{ event.name }}
                </b-link>
              </strong>
            </b-col>
            <b-col>
              Held on:<br />
              <strong>
                <font-awesome-icon icon="fa-solid fa-calendar" class="mr-1" />
                {{ event.date.toLocaleString()
                }}<span v-if="event.endDate !== null">
                  - {{ event.endDate.toLocaleString() }}</span
                ></strong
              >
            </b-col>
            <b-col>
              Songs found:<br />
              <strong>{{ totalEntryCount }}</strong>
            </b-col>
            <b-col class="my-auto">
              <b-dropdown
                :disabled="defaultDisableCondition() || !isActiveMode()"
                block
                :text="getMaxResultsForDisplay()"
                class="my-auto"
                variant="primary"
                menu-class="w-100"
              >
                <b-dropdown-item
                  :disabled="maxResults === 10"
                  @click="setMaxResults(10)"
                  >10
                </b-dropdown-item>
                <b-dropdown-item
                  :disabled="maxResults === 25"
                  @click="setMaxResults(25)"
                  >25
                </b-dropdown-item>
                <b-dropdown-item
                  :disabled="maxResults === 50"
                  @click="setMaxResults(50)"
                  >50
                </b-dropdown-item>
                <b-dropdown-item
                  :disabled="maxResults === 100"
                  @click="setMaxResults(100)"
                  >100
                </b-dropdown-item>
              </b-dropdown>
            </b-col>
            <b-col class="my-auto">
              <b-dropdown
                block
                :disabled="defaultDisableCondition() || !isActiveMode()"
                :text="getOrderingConditionForDisplay()"
                variant="primary"
                menu-class="w-100"
              >
                <b-dropdown-item
                  v-for="(key, value) in orderOptions"
                  :key="key"
                  :disabled="orderingCondition === value"
                  @click="setOrderingCondition(value)"
                >
                  {{ orderOptions[value] }}
                </b-dropdown-item>
              </b-dropdown>
            </b-col>
            <b-col class="my-auto">
              <b-button
                :disabled="defaultDisableCondition()"
                variant="primary"
                block
                :pressed.sync="otherEvents"
                @click="filterEntries()"
                >Songs with other events
              </b-button>
            </b-col>
          </b-row>
        </b-col>
      </b-row>
      <b-row v-if="eventInfoLoaded()" class="col-12">
        <b-col class="my-auto">
          <div class="text-center pt-sm-3">
            <b-button-group>
              <b-button
                v-for="(type, key) in songTypes"
                :key="key"
                class="pl-4 pr-4"
                :disabled="defaultDisableCondition()"
                :variant="
                  (type.show ? '' : 'outline-') +
                  getSongTypeColorForDisplay(type.name)
                "
                @click="
                  type.show = !type.show;
                  filterEntries();
                "
                >{{ getSongTypeStatsForDisplay(type.name) }}
              </b-button>
            </b-button-group>
          </div>
        </b-col>
      </b-row>
      <b-row v-if="eventInfoLoaded" class="col-12">
        <template>
          <div class="overflow-auto m-auto mt-lg-3">
            <b-pagination
              v-model="page"
              align="center"
              :total-rows="totalEntryCount"
              :per-page="maxResults"
              use-router
              first-number
              last-number
              limit="10"
              :disabled="defaultDisableCondition()"
              @change="loadPage"
            />
          </div>
        </template>
      </b-row>
      <b-table-simple
        v-if="eventInfoLoaded() && isActiveMode()"
        hover
        class="mt-1 col-lg-12"
      >
        <b-thead>
          <b-th>
            <b-form-checkbox
              v-model="allChecked"
              :disabled="defaultDisableCondition()"
              size="lg"
              @change="toggleCheckAll"
            />
          </b-th>
          <b-th class="col-3 align-middle">Entry</b-th>
          <b-th class="col-2 align-middle">Current release event</b-th>
          <b-th class="col-3 align-middle">Release date</b-th>
          <b-th class="col-4 align-middle">Proposed actions</b-th>
        </b-thead>
        <b-tbody v-if="!allInvisible()">
          <tr
            v-for="item in entries.filter(item1 => item1.rowVisible)"
            :key="item.songEntry.id"
          >
            <td>
              <b-form-checkbox
                v-if="
                  !item.processed && item.songEntry.eventDateComparison.eligible
                "
                v-model="item.toAssign"
                :disabled="defaultDisableCondition()"
                size="lg"
              />
            </td>
            <td>
              <b-link
                target="_blank"
                :href="getVocaDBEntryUrl(item.songEntry.id)"
                >{{ item.songEntry.name
                }}<b-badge
                  class="badge text-center ml-2"
                  :variant="getSongTypeColorForDisplay(item.songEntry.songType)"
                >
                  {{ getShortenedSongType(item.songEntry.songType) }}
                </b-badge></b-link
              >
              <div class="text-muted">
                {{ item.songEntry.artistString }}
              </div>
            </td>
            <td>
              <span
                v-if="item.songEntry.releaseEvent !== null"
                class="font-weight-bolder"
              >
                <b-badge
                  class="badge text-center"
                  :variant="
                    item.songEntry.releaseEvent.id === event.id
                      ? 'success'
                      : 'warning'
                  "
                  :href="
                    getVocaDBEventUrl(
                      item.songEntry.releaseEvent.id,
                      item.songEntry.releaseEvent.urlSlug
                    )
                  "
                  target="_blank"
                >
                  {{ item.songEntry.releaseEvent.name }}
                </b-badge>
              </span>
              <span v-else class="text-muted">Unspecified</span>
            </td>
            <td>
              <date-disposition
                v-if="item.publishDate !== null"
                :release-date="item.songEntry.publishDate"
                :event-date-comparison="item.songEntry.eventDateComparison"
              />
            </td>
            <td>
              <b-row>
                <b-col cols="10">
                  <ol
                    v-if="
                      !item.processed &&
                      item.songEntry.eventDateComparison.eligible
                    "
                    class="ml-n4"
                  >
                    <li
                      v-if="
                        !item.songEntry.taggedWithMultipleEvents &&
                        item.songEntry.releaseEvent !== null &&
                        item.songEntry.releaseEvent.id !== event.id
                      "
                    >
                      Tag with "<b-link
                        target="_blank"
                        :href="getVocaDBTagUrl(8275, 'multiple-events')"
                        >multiple events</b-link
                      >" and update description
                    </li>
                    <li v-else-if="item.songEntry.releaseEvent == null">
                      Set "<b-link
                        :href="getVocaDBEventUrl(event.id, event.urlSlug)"
                        target="_blank"
                        >{{ event.name }}</b-link
                      >" as release event
                    </li>
                    <li
                      v-else-if="
                        item.songEntry.taggedWithMultipleEvents &&
                        item.songEntry.releaseEvent.id !== event.id
                      "
                    >
                      <span class="text-danger text-monospace">Important:</span>
                      check that description mentions current event
                    </li>
                    <li>
                      Remove tag "<b-link
                        :href="getVocaDBTagUrl(tag.id, tag.urlSlug)"
                        target="_blank"
                        >{{ tag.name }}</b-link
                      >"
                    </li>
                  </ol>
                  <span class="text-muted"
                    >Entry is ineligible for participation</span
                  >
                </b-col>
                <b-col
                  v-if="
                    item.songEntry != null &&
                    item.songEntry.eventDateComparison.eligible
                  "
                >
                  <b-button
                    v-if="item.processed"
                    disabled
                    class="btn"
                    variant="success"
                    @click="processSong(item)"
                  >
                    <font-awesome-icon icon="fas fa-check" />
                  </b-button>
                  <b-button
                    v-else
                    :disabled="defaultDisableCondition()"
                    class="btn"
                    variant="outline-success"
                    @click="processSong(item)"
                  >
                    <font-awesome-icon icon="fas fa-play" />
                  </b-button>
                </b-col>
              </b-row>
            </td>
          </tr>
        </b-tbody>
        <b-tbody v-else>
          <b-tr>
            <b-td colspan="6" class="text-center text-muted">
              <small>No items to display</small>
            </b-td>
          </b-tr>
        </b-tbody>
        <b-tfoot>
          <b-th
            ><b-form-checkbox
              v-model="allChecked"
              size="lg"
              :disabled="defaultDisableCondition()"
              @change="toggleCheckAll"
          /></b-th>
          <b-th class="col-3 align-middle">Entry</b-th>
          <b-th class="col-2 align-middle">Current release event</b-th>
          <b-th class="col-3 align-middle">Release date</b-th>
          <b-th class="col-4 align-middle">Proposed actions</b-th>
        </b-tfoot>
      </b-table-simple>
      <b-row
        v-if="eventInfoLoaded()"
        class="mt-lg-1 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
      >
        <b-col class="col-lg-3 m-auto">
          <b-button
            block
            variant="primary"
            :disabled="countChecked() === 0 || massAssigning || fetching"
            @click="processMultiple"
          >
            <div v-if="massAssigning">
              <b-spinner small class="mr-1"></b-spinner>
              Processing...
            </div>
            <div v-else>Batch process ({{ countChecked() }} selected)</div>
          </b-button>
        </b-col>
      </b-row>

      <b-row v-if="eventInfoLoaded()" class="col-12">
        <template>
          <div class="overflow-auto m-auto my-lg-3">
            <b-pagination
              v-model="page"
              align="center"
              :total-rows="totalEntryCount"
              :per-page="maxResults"
              use-router
              first-number
              last-number
              limit="10"
              :disabled="defaultDisableCondition()"
              @change="loadPage"
            />
          </div>
        </template>
      </b-row>
    </b-row>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { MinimalTag, ReleaseEventForDisplay } from "@/backend/dto";
import { api } from "@/backend";
import { DateTime } from "luxon";
import Component from "vue-class-component";
import { Prop } from "vue-property-decorator";
import {
  getShortenedSongType,
  getVocaDBEventUrl,
  getVocaDBEntryUrl,
  getVocaDBTagUrl,
  getDateDisposition,
  getMaxResultsForDisplay,
  getOrderingConditionForDisplay,
  getSongTypeColorForDisplay,
  getSongTypeStatsForDisplay,
  pageStateIsValid,
  infoLoaded,
  EntryWithReleaseEventAndVisibility,
  SongType,
  getUniqueElementId,
  allVideosInvisible,
  dateIsWithinTimeDelta,
  getTimeDeltaState,
  fillReleaseEventForDisplay,
  DateComparisonResult
} from "@/utils";
import ErrorMessage from "@/components/ErrorMessage.vue";
import DateDisposition from "@/components/DateDisposition.vue";
import { AxiosResponse } from "axios";

@Component({ components: { ErrorMessage, DateDisposition } })
export default class extends Vue {
  @Prop()
  private readonly mode!: string;

  @Prop()
  private readonly thisMode!: string;

  @Prop()
  private readonly targName: string | undefined;

  // main variables
  private event: ReleaseEventForDisplay = {
    id: -1,
    name: "",
    category: "",
    date: null,
    endDate: null,
    urlSlug: "",
    nndTags: []
  };
  private entries: EntryWithReleaseEventAndVisibility[] = [];
  private tag: MinimalTag = { name: "", id: -1, urlSlug: "" };
  private eventTagName: string = "";
  private eventTagNameFrozen: string = "";

  // api variables
  private pageToJump: number = 1;
  private startOffset: number = 0;
  private maxResults: number = 10;
  private orderingCondition = "PublishDate";
  private fetching: boolean = false;
  private massAssigning: boolean = false;
  private assigning: boolean = false;
  private dbAddress: string = "";

  // interface variables
  private allChecked: boolean = false;
  private showCollapse: boolean = false;
  private totalEntryCount: number = 0;
  private timeDeltaEnabled: boolean = true;
  private timeDeltaBefore: boolean = true;
  private timeDeltaAfter: boolean = true;
  private timeDelta: number = 0;
  private timeDeltaMax: number = 0;
  private otherEvents: boolean = true;
  private page: number = 0;
  private maxPage: number = 0;
  private numOfPages: number = 0;

  // error handling
  private alertCode: number = 0;
  private alertMessage: string = "";

  // interface dictionaries
  private readonly orderOptions = {
    PublishDate: "upload time",
    AdditionDate: "addition time",
    RatingScore: "user rating"
  };
  private songTypes: SongType[] = [
    { name: "Unspecified", show: true },
    { name: "Original", show: true },
    { name: "Remaster", show: true },
    { name: "Remix", show: true },
    { name: "Cover", show: true },
    { name: "Instrumental", show: true },
    { name: "Mashup", show: true },
    { name: "MusicPV", show: true },
    { name: "DramaPV", show: true },
    { name: "Other", show: true }
  ];

  // proxy methods
  private getShortenedSongType(songType: string): string {
    return getShortenedSongType(songType);
  }

  private getVocaDBEventUrl(id: number, urlSlug: string): string {
    return getVocaDBEventUrl(this.dbAddress, id, urlSlug);
  }

  private getVocaDBEntryUrl(id: number): string {
    return getVocaDBEntryUrl(this.dbAddress, id);
  }

  private getVocaDBTagUrl(id: number, urlSlug: string): string {
    return getVocaDBTagUrl(this.dbAddress, id, urlSlug);
  }

  private getMaxResultsForDisplay(): string {
    return getMaxResultsForDisplay(this.maxResults);
  }

  private getOrderingConditionForDisplay(): string {
    return getOrderingConditionForDisplay(this.orderingCondition);
  }

  private getSongTypeColorForDisplay(typeString: string): string {
    return getSongTypeColorForDisplay(typeString);
  }

  private getSongTypeStatsForDisplay(type: string): string {
    return getSongTypeStatsForDisplay(
      type,
      this.entries.filter(item => item.songEntry.songType == type).length
    );
  }

  private pageStateIsValid(): boolean {
    return pageStateIsValid(this.pageToJump, this.maxPage);
  }

  private eventInfoLoaded(): boolean {
    return infoLoaded(this.entries.length, this.eventTagNameFrozen);
  }

  private allInvisible(): boolean {
    return allVideosInvisible(this.entries);
  }

  private getDateDisposition(
    date: DateTime | null,
    dateStart: DateTime,
    dateEnd: DateTime | null
  ): DateComparisonResult {
    return getDateDisposition(date, dateStart, dateEnd, this.timeDeltaMax);
  }

  private getTimeDeltaState(): boolean {
    return getTimeDeltaState(
      this.timeDeltaEnabled,
      this.timeDeltaBefore,
      this.timeDeltaAfter,
      this.timeDelta
    );
  }

  // interface methods
  private isActiveMode(): boolean {
    return this.mode == this.thisMode;
  }

  private defaultDisableCondition(): boolean {
    return this.fetching || this.massAssigning || this.assigning;
  }

  private getHiddenTypes(): number {
    return this.songTypes.filter(t => !t.show).length;
  }

  private toggleCheckAll(): void {
    for (const item of this.entries.filter(
      value => value.rowVisible && !value.processed
    )) {
      item.toAssign = this.allChecked;
    }
  }

  private countChecked(): number {
    return this.entries.filter(item => item.toAssign).length;
  }

  private setMaxResults(maxResults: number): void {
    this.maxResults = maxResults;
  }

  private setOrderingCondition(value: string): void {
    this.orderingCondition = value;
  }

  // row filtering
  private hiddenTypeFlag(entry: EntryWithReleaseEventAndVisibility): boolean {
    return (
      this.getHiddenTypes() == 0 ||
      !this.songTypes
        .filter(t => !t.show)
        .map(t => t.name)
        .includes(entry.songEntry.songType)
    );
  }

  private timeDeltaFlag(eventDateComparison: DateComparisonResult): boolean {
    return (
      !this.timeDeltaEnabled ||
      dateIsWithinTimeDelta(
        this.timeDelta,
        this.timeDeltaBefore,
        this.timeDeltaAfter,
        eventDateComparison
      )
    );
  }

  private otherEventsFlag(entry: EntryWithReleaseEventAndVisibility): boolean {
    return (
      this.otherEvents ||
      entry.songEntry.releaseEvent == null ||
      entry.songEntry.releaseEvent.id == this.event.id
    );
  }

  private filterEntries(): void {
    for (const entry of this.entries) {
      entry.rowVisible =
        this.hiddenTypeFlag(entry) &&
        this.timeDeltaFlag(entry.songEntry.eventDateComparison) &&
        this.otherEventsFlag(entry);
      entry.toAssign = entry.toAssign && entry.rowVisible;
    }
  }

  private filterEntriesIfValidState(): void {
    if (this.getTimeDeltaState()) {
      this.filterEntries();
    }
  }

  private updateUrl(): void {
    this.$router
      .push({
        name: "events-full",
        params: { browseMode: this.thisMode, targName: this.eventTagName }
      })
      .catch(err => {
        return false;
      });
  }

  // error handling
  private processError(
    err: { response: AxiosResponse } | { response: undefined; message: string }
  ): void {
    this.$bvToast.show(getUniqueElementId("error_", this.thisMode.toString()));
    if (err.response == undefined) {
      this.alertCode = 0;
      this.alertMessage = err.message;
    } else {
      this.alertCode = err.response.data.code;
      this.alertMessage = err.response.data.message;
    }
  }

  // api methods
  async fetch(
    eventTagName: string,
    newStartOffset: number,
    newPage: number
  ): Promise<void> {
    if (eventTagName == "") {
      return;
    }
    this.showCollapse = false;
    this.fetching = true;
    try {
      localStorage.setItem("max_results", this.maxResults.toString());
      localStorage.setItem("order_by", this.orderingCondition);
      this.pageToJump = newPage;
      let response = await api.fetchEntriesFromDbByEventTag({
        tag: eventTagName,
        startOffset: newStartOffset,
        maxResults: this.maxResults,
        orderBy: this.orderingCondition
      });
      if (response.releaseEvent.date == null) {
        throw { response: undefined, message: "Invalid event date" };
      }
      let entries_temp: EntryWithReleaseEventAndVisibility[] = response.items.map(
        item => {
          return {
            songEntry: item,
            rowVisible: true,
            toAssign: false,
            publishDate:
              item.publishDate !== null
                ? DateTime.fromISO(item.publishDate)
                : null,
            eventDateComparison: null,
            taggedWithMultipleEvents: item.taggedWithMultipleEvents,
            processed: false
          };
        }
      );
      this.totalEntryCount = response.totalCount;
      this.entries = entries_temp;
      this.tag = response.eventTag;
      fillReleaseEventForDisplay(response.releaseEvent, this.event);
      this.entries.forEach(
        value =>
          (value.songEntry.eventDateComparison = this.getDateDisposition(
            value.publishDate,
            this.event.date!,
            this.event.endDate
          ))
      );
      this.filterEntries();
      this.eventTagNameFrozen = this.event.name;
      this.numOfPages = this.totalEntryCount / this.maxResults + 1;
      this.startOffset = newStartOffset;
      this.allChecked = false;
      this.timeDeltaMax = this.event.endDate == null ? 7 : 1;
      this.timeDelta = this.timeDeltaMax;
      localStorage.setItem("vocadb_event_tag_name", eventTagName);
    } catch (err) {
      this.processError(err);
    } finally {
      this.maxPage = Math.ceil(this.totalEntryCount / this.maxResults);
      this.fetching = false;
      this.pageToJump = newPage;
      this.page = newPage;
    }
  }

  private async processSong(
    song: EntryWithReleaseEventAndVisibility
  ): Promise<void> {
    this.assigning = true;
    try {
      await api.assignEventAndRemoveTag({
        songId: song.songEntry.id,
        event: {
          name: this.event.name,
          id: this.event.id,
          urlSlug: this.event.urlSlug
        },
        tagId: this.tag.id
      });
      song.processed = true;
      if (song.songEntry.releaseEvent == null) {
        song.songEntry.releaseEvent = {
          id: this.event.id,
          date: null,
          nndTags: this.event.nndTags,
          name: this.event.name,
          urlSlug: this.event.urlSlug,
          category: this.event.category,
          endDate: null
        };
      }
      song.toAssign = false;
    } catch (err) {
      this.processError(err);
    } finally {
      this.assigning = false;
    }
  }

  private async processMultiple(): Promise<void> {
    this.massAssigning = true;
    try {
      for (const item1 of this.entries.filter(item => item.toAssign)) {
        await this.processSong(item1);
        item1.toAssign = false;
      }
    } finally {
      this.massAssigning = false;
      this.allChecked = false;
    }
  }

  private loadInitialPage(): void {
    this.updateUrl();
    this.fetch(this.eventTagName, 0, 1);
  }

  private loadPage(page: number): void {
    this.updateUrl();
    this.fetch(this.event.name, (page - 1) * this.maxResults, page);
  }

  // session
  created(): void {
    let max_results = localStorage.getItem("max_results");
    if (max_results != null) {
      this.maxResults = parseInt(max_results);
    }
    let order_by = localStorage.getItem("order_by");
    if (order_by != null) {
      this.orderingCondition = order_by;
    }
    if (this.targName != undefined) {
      this.eventTagName = this.targName;
    }
    let event_tag_name = localStorage.getItem("vocadb_event_tag_name");
    if (event_tag_name != null) {
      this.eventTagName = event_tag_name;
    }
    let dbAddress = localStorage.getItem("dbAddress");
    if (this.dbAddress == "" && dbAddress != null) {
      this.dbAddress = dbAddress;
    }
  }

  // fill event tag name from address params (override local storage)
  mounted(): void {
    let targName = this.$route.params["targName"];
    if (targName != undefined) {
      this.eventTagName = targName;
    }
  }
}
</script>
