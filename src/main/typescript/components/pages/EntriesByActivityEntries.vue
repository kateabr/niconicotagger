<template>
  <b-row>
    <progress-bar
      :distinct-song-count="distinctSongCount"
      :fetching="fetching"
      :max-results="maxResults"
    />
    <error-message
      :alert-code="alertCode"
      :alert-message="alertMessage"
      :this-mode="thisMode"
    />
    <div
      class="py-lg-3 px-lg-4 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
    >
      <b-row>
        <b-col class="my-auto">
          <b-dropdown
            :disabled="
              defaultDisableCondition() ||
              songsInfoLoaded() ||
              (isActiveMode() && sessionLocked)
            "
            block
            :text="getMaxResultsForDisplay()"
            class="my-auto"
            variant="primary"
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
            :disabled="
              defaultDisableCondition() ||
              songsInfoLoaded() ||
              (isActiveMode() && sessionLocked)
            "
            :text="getSortingConditionForDisplay()"
            variant="primary"
          >
            <b-dropdown-item
              v-for="(key, value) in sortingOptions"
              :key="key"
              :disabled="sortingCondition === value"
              @click="setSortingOption(value)"
            >
              {{ sortingOptions[value] }}
            </b-dropdown-item>
          </b-dropdown>
        </b-col>
        <b-col class="m-auto">
          <b-button
            v-if="!sessionLocked || videos.length > 0"
            variant="primary"
            block
            :disabled="defaultDisableCondition() || !timestampIsValid"
            @click="
              fetch('right', {
                mode: additionMode,
                createDate: timestamp,
                id: additionMode === 'since' ? 0 : 10000000,
                sortRule: sortingCondition,
                reverse: false
              })
            "
            ><span v-if="fetching"><b-spinner small /></span>
            <span v-else>
              <span v-if="!songsInfoLoaded()">Load</span>
              <span v-else>Reload</span>
            </span>
          </b-button>
          <b-button-group v-else class="btn-group-justified w-100">
            <b-button
              block
              variant="success"
              :disabled="defaultDisableCondition()"
              @click="
                fetch('right', {
                  mode: additionMode,
                  createDate: timestamp,
                  id: additionMode === 'since' ? 0 : 10000000,
                  sortRule: sortingCondition,
                  reverse: false
                })
              "
              ><span v-if="fetching"><b-spinner small /></span>
              <span v-else
                >Restore ({{ additionMode }}
                {{ timestamp.split("T")[0] }})</span
              >
            </b-button>
            <b-button
              style="width: 80px"
              variant="danger"
              :disabled="defaultDisableCondition()"
              @click="unlockSession()"
            >
              Clear
            </b-button>
          </b-button-group>
        </b-col>
        <b-col class="my-auto">
          <b-button
            variant="primary"
            block
            :disabled="defaultDisableCondition()"
            :pressed.sync="showEntriesWithErrors"
            @click="filterVideos()"
            >Entries with errors
          </b-button>
        </b-col>
        <b-col class="my-auto">
          <b-button
            variant="primary"
            block
            :pressed.sync="showEntriesWithNoTags"
            :disabled="defaultDisableCondition()"
            @click="filterVideos()"
            >Entries with no tags to add
          </b-button>
        </b-col>
      </b-row>
      <b-row class="mt-3">
        <b-col cols="2" class="my-auto"></b-col>
        <b-col class="my-auto">
          <template>
            <b-input-group inline>
              <b-form-input
                v-model="timestamp"
                :readonly="fetching || (isActiveMode() && sessionLocked)"
                type="text"
                placeholder="Specify timestamp"
                :state="validateTimestamp()"
                @input="timestampPickerIsDisabled = true"
              />
              <template #prepend>
                <b-form-datepicker
                  v-model="dateBound"
                  :max="now"
                  :min="min"
                  :initial-date="now"
                  locale="en"
                  button-only
                  :disabled="
                    fetching ||
                    timestampPickerIsDisabled ||
                    (isActiveMode() && sessionLocked)
                  "
                  hide-header
                  style="width: 80px"
                  @input="refreshTimestamp()"
                />
              </template>
              <template #append>
                <b-button
                  style="width: 80px"
                  variant="danger"
                  :disabled="
                    timestamp === '' ||
                    fetching ||
                    (isActiveMode() && sessionLocked)
                  "
                  @click="clearTimestamp"
                >
                  Clear
                </b-button>
              </template>
            </b-input-group>
          </template>
        </b-col>
        <b-col class="my-auto text-left align-middle">
          <b-dropdown
            block
            :disabled="
              defaultDisableCondition() ||
              songsInfoLoaded() ||
              (isActiveMode() && sessionLocked)
            "
            :text="getAdditionModeForDisplay()"
            variant="primary"
          >
            <b-dropdown-item
              v-for="(key, value) in additionOptions"
              :key="key"
              :disabled="additionMode === value"
              @click="setAdditionMode(value)"
            >
              {{ additionOptions[value] }}
            </b-dropdown-item>
          </b-dropdown>
        </b-col>
        <b-col cols="2" class="my-auto"></b-col>
      </b-row>
    </div>
    <b-row v-if="songsInfoLoaded()" class="col-12">
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
                filterVideos();
              "
              >{{ getSongTypeStatsForDisplay(type.name) }}
            </b-button>
          </b-button-group>
        </div>
      </b-col>
    </b-row>
    <b-row v-if="songsInfoLoaded()" class="flex-fill text-center">
      <div class="overflow-auto mx-auto flex-fill my-3">
        <b-button-group class="my-1">
          <b-button
            variant="link"
            :disabled="defaultDisableCondition() || !timestampIsValid"
            class="text-left pl-3"
            @click="
              fetch(
                'left',
                getLeftButtonPayload(
                  sortingCondition,
                  videos[0].song.createDate,
                  videos[0].song.id
                )
              )
            "
          >
            <font-awesome-icon icon="fa-solid fa-less-than" class="mr-3" />
            {{ sortingCondition === "CreateDate" ? "Older" : "Newer" }}
          </b-button>
          <b-button
            variant="link"
            :disabled="defaultDisableCondition() || !timestampIsValid"
            class="text-right pr-3"
            @click="
              fetch(
                'right',
                getRightButtonPayload(
                  sortingCondition,
                  videos[videos.length - 1].song.createDate,
                  videos[videos.length - 1].song.id
                )
              )
            "
            >{{ sortingCondition === "CreateDate" ? "Newer" : "Older" }}
            <font-awesome-icon icon="fa-solid fa-greater-than" class="ml-3" />
          </b-button>
        </b-button-group>
      </div>
    </b-row>
    <b-table-simple
      v-if="songsInfoLoaded() && isActiveMode()"
      hover
      class="mt-1 col-lg-12"
    >
      <b-thead>
        <b-th>
          <b-form-checkbox class="invisible" size="lg"></b-form-checkbox>
        </b-th>
        <b-th class="col-3 align-middle">Entry</b-th>
        <b-th class="col-9 align-middle">Videos</b-th>
      </b-thead>
      <b-tbody v-if="videos.filter(video => video.visible).length > 0">
        <tr
          v-for="video in videos.filter(vid => vid.visible)"
          :key="video.song.id"
        >
          <td>
            <div v-if="video.thumbnailsOk.length > 0">
              <b-form-checkbox
                v-if="video.tagsToAssign.length > 0"
                v-model="video.toAssign"
                size="lg"
                :disabled="defaultDisableCondition()"
              ></b-form-checkbox>
            </div>
          </td>
          <td>
            <b-link target="_blank" :href="getVocaDBEntryUrl(video.song.id)"
              >{{ video.song.name
              }}<b-badge
                class="badge text-center ml-2"
                :variant="getSongTypeColorForDisplay(video.song.songType)"
              >
                {{ getShortenedSongType(video.song.songType) }}
              </b-badge></b-link
            >
            <b-badge
              v-clipboard:copy="video.song.createDate"
              class="m-sm-1"
              href="#"
              variant="light"
            >
              <font-awesome-icon icon="fa-solid fa-calendar" class="mr-1" />
              {{ video.song.createDate.split("T")[0] }}
            </b-badge>
            <div class="text-muted">
              {{ video.song.artistString }}
            </div>
          </td>
          <td>
            <b-row
              v-for="(thumbnail, thumbnail_key) in video.thumbnailsOk"
              :key="thumbnail_key"
            >
              <b-col class="col-8">
                <b-button
                  :disabled="defaultDisableCondition()"
                  size="sm"
                  variant="primary-outline"
                  class="mr-2"
                  @click="thumbnail.expanded = !thumbnail.expanded"
                >
                  <font-awesome-icon icon="fas fa-play" />
                </b-button>
                <b-link
                  target="_blank"
                  :href="getNicoVideoUrl(thumbnail.thumbnail.id)"
                  >{{ thumbnail.thumbnail.title }}</b-link
                >
                <div>
                  <b-badge
                    v-for="(nico_tag, key) in thumbnail.nicoTags"
                    :key="key"
                    v-clipboard:copy="nico_tag.name"
                    class="m-sm-1"
                    href="#"
                    :variant="nico_tag.variant"
                    >{{ nico_tag.name }}
                    <font-awesome-icon
                      v-if="nico_tag.locked"
                      icon="fas fa-lock"
                      class="ml-1"
                    />
                  </b-badge>
                </div>
                <b-collapse
                  :id="getCollapseId(thumbnail.thumbnail.id)"
                  :visible="thumbnail.expanded && !fetching"
                  class="mt-2 collapsed"
                >
                  <nico-embed :content-id="thumbnail.thumbnail.id" />
                </b-collapse>
              </b-col>
              <b-col>
                <span
                  v-for="(tag, tag_key) in thumbnail.mappedTags"
                  :key="tag_key"
                >
                  <b-button
                    size="sm"
                    class="m-1"
                    :disabled="tag.assigned || defaultDisableCondition()"
                    :variant="getTagVariant(tag, video.tagsToAssign)"
                    @click="toggleTagAssignation(tag, video)"
                  >
                    <font-awesome-icon
                      :icon="getTagIcon(tag, video.tagsToAssign)"
                      class="sm mr-sm-1"
                    />
                    {{ tag.tag.name }}
                  </b-button>
                </span>
                <div
                  v-if="thumbnail.mappedTags.length === 0"
                  class="text-muted"
                >
                  No mapped tags available for this video
                </div>
              </b-col>
            </b-row>
            <b-row
              v-for="(thumbnail, thumbnail_err_key) in video.thumbnailsErr"
              :key="thumbnail_err_key"
            >
              <b-col>
                <b-link :href="getDeletedVideoUrl(thumbnail.id)" target="_blank"
                  >{{ thumbnail.title }}
                </b-link>
                <span
                  ><b-badge variant="danger" size="sm" class="ml-1">{{
                    thumbnail.code
                  }}</b-badge></span
                >
                <div>
                  <span
                    ><b-badge
                      v-if="thumbnail.code === 'DELETED'"
                      variant="warning"
                      class="m-1"
                    >
                      Needs to be disabled</b-badge
                    >
                    <b-badge
                      v-else-if="
                        thumbnail.code === 'COMMUNITY' && !thumbnail.community
                      "
                      variant="warning"
                      class="m-1"
                    >
                      Needs to be tagged with
                      <b-link
                        target="_blank"
                        :href="
                          getVocaDBTagUrl(
                            niconicoCommunityExclusive.id,
                            niconicoCommunityExclusive.urlSlug
                          )
                        "
                        >{{ niconicoCommunityExclusive.name }}</b-link
                      ></b-badge
                    >
                    <b-badge
                      v-else-if="
                        thumbnail.code === 'COMMUNITY' && thumbnail.community
                      "
                      variant="success"
                      class="m-1"
                    >
                      Tagged with
                      <b-link
                        target="_blank"
                        :href="
                          getVocaDBTagUrl(
                            niconicoCommunityExclusive.id,
                            niconicoCommunityExclusive.urlSlug
                          )
                        "
                        >{{ niconicoCommunityExclusive.name }}</b-link
                      ></b-badge
                    >
                    <b-badge v-else variant="warning" class="m-1">
                      Unknown error</b-badge
                    >
                  </span>
                </div>
              </b-col>
              <b-col
                v-if="thumbnail.code === 'COMMUNITY' && !thumbnail.community"
                class="col-4"
              >
                <b-button size="sm" class="m-1" disabled variant="warning">
                  <font-awesome-icon
                    :icon="['fas', 'fa-minus']"
                    class="sm mr-sm-1"
                  />
                  {{ niconicoCommunityExclusive.name }}
                </b-button>
              </b-col>
            </b-row>
            <b-row
              v-if="
                video.thumbnailsOk.length === 0 &&
                video.thumbnailsErr.length === 0
              "
            >
              <b-col class="text-muted">
                No NicoNicoDouga uploads associated with this entry
              </b-col>
            </b-row>
          </td>
        </tr>
      </b-tbody>
      <b-tbody v-else>
        <b-tr>
          <b-td colspan="4" class="text-center text-muted">
            <small>No items to display</small>
          </b-td>
        </b-tr>
      </b-tbody>
      <b-tfoot>
        <b-th></b-th>
        <b-th class="col-3 align-middle">Entry</b-th>
        <b-th class="col-9 align-middle">Videos</b-th>
      </b-tfoot>
    </b-table-simple>
    <b-row
      v-if="songsInfoLoaded()"
      class="mt-lg-1 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
    >
      <b-col class="col-lg-3 m-auto">
        <b-button
          block
          variant="primary"
          :disabled="countChecked() === 0 || massAssigning || fetching"
          @click="assignMultiple"
        >
          <div v-if="massAssigning">
            <b-spinner small class="mr-1"></b-spinner>
            Assigning...
          </div>
          <div v-else>Batch assign ({{ countChecked() }} selected)</div>
        </b-button>
      </b-col>
    </b-row>
    <b-row v-if="songsInfoLoaded()" class="flex-fill text-center mb-3">
      <div class="overflow-auto mx-auto flex-fill my-3">
        <b-button-group class="my-1">
          <b-button
            variant="link"
            :disabled="defaultDisableCondition() || !timestampIsValid"
            class="text-left pl-3"
            @click="
              fetch(
                'left',
                getLeftButtonPayload(
                  sortingCondition,
                  videos[0].song.createDate,
                  videos[0].song.id
                )
              )
            "
          >
            <font-awesome-icon icon="fa-solid fa-less-than" class="mr-3" />
            {{ sortingCondition === "CreateDate" ? "Older" : "Newer" }}
          </b-button>
          <b-button
            variant="link"
            :disabled="defaultDisableCondition() || !timestampIsValid"
            class="text-right pr-3"
            @click="
              fetch(
                'right',
                getRightButtonPayload(
                  sortingCondition,
                  videos[videos.length - 1].song.createDate,
                  videos[videos.length - 1].song.id
                )
              )
            "
            >{{ sortingCondition === "CreateDate" ? "Newer" : "Older" }}
            <font-awesome-icon icon="fa-solid fa-greater-than" class="ml-3" />
          </b-button>
        </b-button-group>
      </div>
    </b-row>
  </b-row>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import {
  EntryWithVideosAndVisibility,
  getAdditionModeForDisplay,
  getMaxResultsForDisplay,
  getSortingConditionForDisplay,
  SongType,
  validateTimestamp,
  getSongTypeColorForDisplay,
  getSongTypeStatsForDisplay,
  getLeftButtonPayload,
  getRightButtonPayload,
  Fetch1Payload,
  updateFetch1Payload,
  getVocaDBEntryUrl,
  getNicoVideoUrl,
  getShortenedSongType,
  getUniqueElementId,
  getTagVariant,
  toggleTagAssignation,
  getTagIconForTagAssignationButton,
  getDeletedVideoUrl,
  getVocaDBTagUrl
} from "@/utils";
import { DateTime } from "luxon";
import { api } from "@/backend";
import { MappedTag, MinimalTag } from "@/backend/dto";
import NicoEmbed from "@/components/NicoEmbed.vue";
import ProgressBar from "@/components/ProgressBar.vue";
import ErrorMessage from "@/components/ErrorMessage.vue";
import { AxiosResponse } from "axios";

@Component({ components: { NicoEmbed, ProgressBar, ErrorMessage } })
export default class extends Vue {
  @Prop()
  private readonly mode!: string;

  @Prop()
  private readonly thisMode!: string;

  // main variables
  private videos: EntryWithVideosAndVisibility[] = [];

  // api variables
  private maxResults: number = 0;
  private additionMode = "before";
  private sortingCondition: string = "CreateDateDescending";
  private timestamp = "";

  // interface variables
  private fetching: boolean = false;
  private massAssigning: boolean = false;
  private totalVideoCount: number = 0;
  private maxPage: number = 0;
  private numOfPages: number = 1;
  private dateBound = "";
  private now = new Date();
  private min = new Date("2012-02-23T09:58:03");
  private timestampIsValid = false;
  private timestampPickerIsDisabled = false;
  private distinctSongCount: number = 0;
  private showEntriesWithNoTags: boolean = false;
  private showEntriesWithErrors: boolean = true;
  private sessionLocked: boolean = false;

  // error handling
  private alertCode: number = 0;
  private alertMessage: string = "";

  // interface constants
  private sortingOptions = {
    CreateDate: "old→new",
    CreateDateDescending: "new→old"
  };

  private additionOptions = {
    before: "before",
    since: "since"
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

  private songTypeToTag = {
    Unspecified: [],
    Original: [6479],
    Remaster: [1519, 391, 371, 392, 74],
    Remix: [371, 74, 391],
    Cover: [74, 371, 392],
    Instrumental: [208],
    MusicPV: [7378, 74, 4582],
    Mashup: [3392],
    DramaPV: [
      104,
      1736,
      7276,
      3180,
      7728,
      8509,
      7748,
      7275,
      6701,
      3186,
      8130,
      6700,
      7615,
      6703,
      6702,
      7988,
      6650,
      8043,
      8409,
      423
    ],
    Other: []
  };

  private niconicoCommunityExclusive: MinimalTag = {
    id: 7446,
    name: "niconico community exclusive",
    urlSlug: "niconico-community-exclusive"
  };

  //proxy methods
  private getMaxResultsForDisplay(): string {
    return getMaxResultsForDisplay(this.maxResults);
  }

  private getSortingConditionForDisplay() {
    return getSortingConditionForDisplay(this.sortingCondition);
  }

  private getSongTypeColorForDisplay(typeString: string): string {
    return getSongTypeColorForDisplay(typeString);
  }

  private getCollapseId(contentId: string): string {
    return getUniqueElementId("collapse_", contentId);
  }

  private getTagVariant(tag: MappedTag, tagsToAssign: MinimalTag[]): string {
    return getTagVariant(tag, tagsToAssign);
  }

  private getTagIcon(tag: MappedTag, tagsToAssign: MinimalTag[]): string[] {
    return getTagIconForTagAssignationButton(tag, tagsToAssign);
  }

  private getLeftButtonPayload(
    sortingCondition: string,
    creationDate: string,
    id: number
  ): Fetch1Payload {
    return getLeftButtonPayload(sortingCondition, creationDate, id);
  }

  private getRightButtonPayload(
    sortingCondition: string,
    creationDate: string,
    id: number
  ): Fetch1Payload {
    return getRightButtonPayload(sortingCondition, creationDate, id);
  }

  private getShortenedSongType(songType: string): string {
    return getShortenedSongType(songType);
  }

  private getVocaDBEntryUrl(id: number): string {
    return getVocaDBEntryUrl(id);
  }

  private getVocaDBTagUrl(id: number, urlSlug: string): string {
    return getVocaDBTagUrl(id, urlSlug);
  }

  private getNicoVideoUrl(contentId: string): string {
    return getNicoVideoUrl(contentId);
  }

  private getDeletedVideoUrl(videoId: string): string {
    return getDeletedVideoUrl(videoId);
  }

  private validateTimestamp(): boolean | null {
    const res = validateTimestamp(this.timestamp);
    if (res != null) {
      this.timestampIsValid = res;
    }
    return res;
  }

  // interface methods
  private isActiveMode(): boolean {
    return this.mode == this.thisMode;
  }

  private defaultDisableCondition(): boolean {
    return this.fetching || this.massAssigning;
  }

  private setAdditionMode(value: string): void {
    this.additionMode = value;
  }

  private setMaxResults(maxResults: number): void {
    this.maxResults = maxResults;
  }

  private setSortingOption(value: string): void {
    this.sortingCondition = value;
  }

  private getHiddenTypes(): number {
    return this.songTypes.filter(t => !t.show).length;
  }

  private getAdditionModeForDisplay() {
    return getAdditionModeForDisplay(this.additionMode);
  }

  private getSongTypeStatsForDisplay(type: string): string {
    return getSongTypeStatsForDisplay(
      type,
      this.videos.filter(item => item.song.songType == type).length
    );
  }

  private songsInfoLoaded(): boolean {
    return this.videos.length > 0 && this.totalVideoCount > 0;
  }

  private countChecked(): number {
    return this.videos.filter(video => video.toAssign).length;
  }

  private toggleTagAssignation(
    tag: MappedTag,
    video: EntryWithVideosAndVisibility
  ): void {
    toggleTagAssignation(tag, video);
  }

  private refreshTimestamp() {
    const temp = DateTime.fromISO(this.dateBound + "T00:00:00")
      .toJSON()
      .split("+")[0];
    if (this.timestamp !== temp) {
      this.timestamp = temp;
    }
  }

  private clearTimestamp() {
    this.dateBound = "";
    this.timestamp = "";
    this.timestampIsValid = false;
    this.videos = [];
    this.timestampPickerIsDisabled = false;
  }

  // row filtering
  private hiddenTypeFlag(video: EntryWithVideosAndVisibility): boolean {
    return (
      this.getHiddenTypes() == 0 ||
      !this.songTypes
        .filter(t => !t.show)
        .map(t => t.name)
        .includes(video.song.songType)
    );
  }

  private hasTagsToAssign(entry: EntryWithVideosAndVisibility): boolean {
    let assignable_mapped_tags_cnt = 0;
    for (const thumbnailOk of entry.thumbnailsOk) {
      assignable_mapped_tags_cnt += thumbnailOk.mappedTags.filter(
        tag => !tag.assigned
      ).length;
    }
    return assignable_mapped_tags_cnt > 0;
  }

  private hideEntriesWithNoTagsFlag(
    entry: EntryWithVideosAndVisibility
  ): boolean {
    return this.showEntriesWithNoTags || this.hasTagsToAssign(entry);
  }

  private showEntriesWithErrorsFlag(
    entry: EntryWithVideosAndVisibility
  ): boolean {
    return (
      this.showEntriesWithErrors &&
      entry.thumbnailsErr.filter(thumb => !thumb.community).length > 0
    );
  }

  private filterVideos(): void {
    for (const video of this.videos) {
      video.visible =
        (this.hiddenTypeFlag(video) && this.hideEntriesWithNoTagsFlag(video)) ||
        this.showEntriesWithErrorsFlag(video);
    }
  }

  private updateUrl(): void {
    this.$router
      .push({
        name: "tags-mode",
        params: { browseMode: this.thisMode }
      })
      .catch(err => {
        return false;
      });
  }

  // api methods
  async fetch(direction: string, payload: Fetch1Payload): Promise<void> {
    this.updateUrl();
    this.fetching = true;
    this.timestamp = payload.createDate;
    this.additionMode = payload.mode;
    localStorage.setItem("max_results", this.maxResults.toString());
    localStorage.setItem("timestamp", this.timestamp);
    localStorage.setItem("sort_by_vocadb", this.sortingCondition);
    localStorage.setItem("added_mode", this.additionMode);
    const reverse = payload.reverse;
    try {
      let videos: EntryWithVideosAndVisibility[] = [];
      this.distinctSongCount = 0;
      let end = false;
      while (this.distinctSongCount < this.maxResults && !end) {
        let response = await api.fetchVideosFromDbBeforeSince({
          maxResults: 10,
          mode: payload.mode,
          dateTime: payload.createDate,
          songId: payload.id,
          sortRule: payload.sortRule
        });
        let videos_temp: EntryWithVideosAndVisibility[] = response.items.map(
          entry => {
            let commEx: boolean =
              entry.thumbnailsErr.filter(
                thumb => thumb.code == "COMMUNITY" && !thumb.community
              ).length > 0;
            return {
              song: entry.song,
              toAssign: commEx,
              visible: true,
              thumbnailsOk: entry.thumbnailsOk.map(t => {
                return {
                  thumbnail: t.thumbnail,
                  mappedTags: t.mappedTags.map(tag => {
                    return {
                      tag: tag.tag,
                      assigned: tag.assigned,
                      toAssign: false
                    };
                  }),
                  expanded: false,
                  nicoTags: t.nicoTags
                };
              }),
              thumbnailsErr: entry.thumbnailsErr,
              tagsToAssign: commEx ? [this.niconicoCommunityExclusive] : []
            };
          }
        );

        end =
          response.timestampFirst === response.timestampLast &&
          response.items.length === 1;

        if (this.distinctSongCount > 0) {
          const overlap = videos_temp.find(
            video => video.song.id == videos[videos.length - 1].song.id
          );
          if (overlap != undefined) {
            videos_temp.splice(0, videos_temp.indexOf(overlap) + 1);
          }
        }

        this.totalVideoCount = response.totalCount;

        this.distinctSongCount += videos_temp.length;

        videos = videos.concat(videos_temp);

        // update payload
        payload = updateFetch1Payload(
          videos_temp,
          this.sortingCondition,
          payload.sortRule,
          response.timestampFirst,
          response.timestampLast,
          this.additionMode,
          direction
        );
      }
      videos.splice(this.maxResults);
      if (reverse) {
        videos = videos.reverse();
      }
      this.videos = videos;
      this.postProcessVideos();
      this.filterVideos();
      this.distinctSongCount = 100;
      this.numOfPages = this.totalVideoCount / this.maxResults + 1;
    } catch (err) {
      this.processError(err);
    } finally {
      this.maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
      this.fetching = false;
    }
  }

  private async assignMultiple(): Promise<void> {
    this.massAssigning = true;
    try {
      for (const song of this.videos.filter(s => s.toAssign)) {
        await api.lookUpAndAssignTag({
          tags: song.tagsToAssign,
          songId: song.song.id
        });
        song.toAssign = false;
        const assigned_ids = song.tagsToAssign.map(tta => tta.id);
        for (const thumbnailOk of song.thumbnailsOk) {
          thumbnailOk.mappedTags.forEach(t => {
            if (assigned_ids.find(id => id == t.tag.id) != undefined) {
              t.toAssign = false;
              t.assigned = true;
            }
          });
        }
        if (
          assigned_ids.find(id => id == this.niconicoCommunityExclusive.id) !=
          undefined
        ) {
          song.thumbnailsErr.forEach(thumbnailErr => {
            if (thumbnailErr.code == "COMMUNITY") {
              thumbnailErr.community = true;
            }
          });
        }
        song.tagsToAssign.splice(0, song.tagsToAssign.length);
      }
    } catch (err) {
      this.processError(err);
    } finally {
      this.massAssigning = false;
    }
  }

  private postProcessVideos() {
    for (const video of this.videos) {
      for (const thumbnailOk of video.thumbnailsOk) {
        thumbnailOk.mappedTags = thumbnailOk.mappedTags.filter(
          t =>
            this.songTypeToTag[video.song.songType].find(
              (id: number) => id == t.tag.id
            ) == undefined
        );
        thumbnailOk.mappedTags = thumbnailOk.mappedTags.filter(function (
          elem,
          index,
          self
        ) {
          return (
            index ===
            self.indexOf(self.find(el => el.tag.name == elem.tag.name)!)
          );
        });
      }
    }
  }

  // error handling
  private processError(
    err: { response: AxiosResponse } | { response: undefined; message: string }
  ): void {
    this.$bvToast.show("error_" + this.thisMode);
    if (err.response == undefined) {
      this.alertCode = 0;
      this.alertMessage = err.message;
    } else {
      this.alertCode = err.response.data.code;
      this.alertMessage = err.response.data.message;
    }
  }

  // session
  created(): void {
    let max_results = localStorage.getItem("max_results");
    if (max_results != null) {
      this.maxResults = parseInt(max_results);
    }
    let timestamp = localStorage.getItem("timestamp");
    if (timestamp != null) {
      this.timestamp = timestamp;
      this.timestampIsValid = true;
    }
    let sort_by = localStorage.getItem("sort_by_vocadb");
    if (sort_by != null) {
      this.sortingCondition = sort_by;
    }
    let added_mode = localStorage.getItem("added_mode");
    if (added_mode != null) {
      this.additionMode = added_mode;
    }
    if (
      max_results != null &&
      timestamp != null &&
      sort_by != null &&
      added_mode != null
    ) {
      this.sessionLocked = true;
    }
  }

  private unlockSession(): void {
    this.sessionLocked = false;
    this.timestampIsValid = true;
  }
}
</script>
