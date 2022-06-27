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
    <b-row
      class="pt-lg-3 pb-lg-3 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
    >
      <b-col class="my-auto">
        <b-dropdown
          :disabled="defaultDisableCondition() || sessionLocked"
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
          :disabled="defaultDisableCondition() || sessionLocked"
          :text="getOrderingConditionForDisplay()"
          variant="primary"
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
      <b-col v-if="songsInfoLoaded()" class="my-auto">
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
                  pageStateIsValid()
                    ? fetch((pageToJump - 1) * maxResults, pageToJump)
                    : null
                "
              >
              </b-form-input>
            </template>
            <template #append>
              <b-button
                style="width: 80px"
                :variant="pageStateIsValid() ? 'success' : 'danger'"
                :disabled="defaultDisableCondition() || !pageStateIsValid()"
                @click="loadPage(page)"
              >
                <span v-if="fetching"><b-spinner small /></span>
                <span v-else-if="pageToJump === page">Refresh</span>
                <span v-else>Jump</span>
              </b-button>
            </template>
          </b-input-group>
        </template>
      </b-col>
      <b-col v-else class="m-auto">
        <b-button
          v-if="!sessionLocked && videos.length === 0"
          variant="primary"
          block
          :disabled="defaultDisableCondition()"
          @click="fetch(0, 1)"
          ><span v-if="fetching"><b-spinner small /></span>
          <span v-else>Load</span>
        </b-button>
        <b-button-group v-else class="btn-group-justified w-100">
          <b-button
            block
            variant="success"
            :disabled="defaultDisableCondition()"
            @click="fetch(startOffset, startOffset / maxResults + 1)"
            ><span v-if="fetching"><b-spinner small /></span>
            <span v-else>Restore page {{ startOffset / maxResults + 1 }}</span>
          </b-button>
          <b-button
            style="width: 80px"
            variant="danger"
            :disabled="defaultDisableCondition()"
            @click="unlockSession"
          >
            Clear
          </b-button>
        </b-button-group>
      </b-col>
      <b-col class="my-auto">
        <b-button
          variant="primary"
          block
          :pressed="showEntriesWithErrors"
          :disabled="defaultDisableCondition()"
          @click="toggleShowEntriesWithErrors"
          >Entries with errors
        </b-button>
      </b-col>
      <b-col class="my-auto">
        <b-button
          variant="primary"
          block
          :pressed="!hideEntriesWithNoTags"
          :disabled="defaultDisableCondition()"
          @click="toggleHideEntriesWithNoTags"
          >Entries with no tags to add
        </b-button>
      </b-col>
    </b-row>
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
    <b-row v-if="songsInfoLoaded()" class="col-12">
      <template>
        <div class="overflow-auto m-auto mt-lg-3">
          <b-pagination
            v-model="page"
            align="center"
            :total-rows="totalVideoCount"
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
            <b-link
              target="_blank"
              :href="getVocaDBEntryUrl(video.song.id)"
              v-html="video.song.name"
            />
            <b-link target="_blank" :href="getVocaDBEntryUrl(video.song.id)">
              <b-badge
                class="badge text-center ml-2"
                :variant="getSongTypeColorForDisplay(video.song.songType)"
              >
                {{ getShortenedSongType(video.song.songType) }}
              </b-badge>
            </b-link>
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
                  v-html="thumbnail.thumbnail.title"
                />
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

    <b-row v-if="songsInfoLoaded()" class="col-12">
      <template>
        <div class="overflow-auto m-auto my-lg-3">
          <b-pagination
            v-model="page"
            align="center"
            :total-rows="totalVideoCount"
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
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import {
  getMaxResultsForDisplay,
  getOrderingConditionForDisplay,
  pageStateIsValid,
  getSongTypeColorForDisplay,
  getSongTypeStatsForDisplay,
  getVocaDBEntryUrl,
  getUniqueElementId,
  getTagVariant,
  getShortenedSongType,
  toggleTagAssignation,
  getTagIconForTagAssignationButton,
  getDeletedVideoUrl,
  getVocaDBTagUrl,
  EntryWithVideosAndVisibility,
  SongType,
  getNicoVideoUrl
} from "@/utils";
import { api } from "@/backend";
import { MappedTag, MinimalTag } from "@/backend/dto";
import NicoEmbed from "@/components/NicoEmbed.vue";
import ProgressBar from "@/components/ProgressBar.vue";
import ErrorMessage from "@/components/ErrorMessage.vue";

@Component({ components: { ProgressBar, NicoEmbed, ErrorMessage } })
export default class extends Vue {
  @Prop()
  private readonly mode!: number;

  @Prop()
  private readonly thisMode!: number;

  // main variables
  private videos: EntryWithVideosAndVisibility[] = [];

  // api variables
  private maxResults: number = 10;
  private orderingCondition: string = "AdditionDate";
  private pageToJump: number = 0;
  private startOffset: number = 0;

  // interface variables
  private fetching: boolean = false;
  private massAssigning: boolean = false;
  private totalVideoCount: number = 0;
  private maxPage: number = 0;
  private numOfPages: number = 1;
  private page: number = 1;
  private distinctSongCount: number = 0;
  private hideEntriesWithNoTags: boolean = false;
  private showEntriesWithErrors: boolean = true;
  private sessionLocked: boolean = false;

  // interface constants
  private orderOptions = {
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

  // error handling
  private alertCode: number = 0;
  private alertMessage: string = "";

  // interface methods
  private isActiveMode(): boolean {
    return this.mode == this.thisMode;
  }

  private setMaxResults(maxResults: number): void {
    this.maxResults = maxResults;
  }

  private setOrderingCondition(value: string): void {
    this.orderingCondition = value;
  }

  private getHiddenTypes(): number {
    return this.songTypes.filter(t => !t.show).length;
  }

  private toggleShowEntriesWithErrors() {
    this.showEntriesWithErrors = !this.showEntriesWithErrors;
    this.filterVideos();
  }

  private toggleHideEntriesWithNoTags() {
    this.hideEntriesWithNoTags = !this.hideEntriesWithNoTags;
    this.filterVideos();
  }

  private countChecked(): number {
    return this.videos.filter(video => video.toAssign).length;
  }

  // row filtering
  private hiddenTypeFlag(entry: EntryWithVideosAndVisibility): boolean {
    return (
      this.getHiddenTypes() == 0 ||
      !this.songTypes
        .filter(t => !t.show)
        .map(t => t.name)
        .includes(entry.song.songType)
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
    return !this.hideEntriesWithNoTags || this.hasTagsToAssign(entry);
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

  // proxy methods
  private getMaxResultsForDisplay(): string {
    return getMaxResultsForDisplay(this.maxResults);
  }
  private getOrderingConditionForDisplay(): string {
    return getOrderingConditionForDisplay(this.orderingCondition);
  }

  private defaultDisableCondition(): boolean {
    return this.fetching || this.massAssigning;
  }

  private songsInfoLoaded(): boolean {
    return this.videos.length > 0 && this.totalVideoCount > 0;
  }

  private pageStateIsValid(): boolean {
    return pageStateIsValid(this.pageToJump, this.maxPage);
  }

  private getSongTypeColorForDisplay(typeString: string): string {
    return getSongTypeColorForDisplay(typeString);
  }

  private getVocaDBEntryUrl(id: number): string {
    return getVocaDBEntryUrl(id);
  }

  private getVocaDBTagUrl(id: number, urlSlug: string): string {
    return getVocaDBTagUrl(id, urlSlug);
  }

  private getDeletedVideoUrl(videoId: string): string {
    return getDeletedVideoUrl(videoId);
  }

  private getNicoVideoUrl(contentId: string): string {
    return getNicoVideoUrl(contentId);
  }

  private getSongTypeStatsForDisplay(type: string): string {
    return getSongTypeStatsForDisplay(
      type,
      this.videos.filter(item => item.song.songType == type).length
    );
  }

  private getShortenedSongType(songType: string): string {
    return getShortenedSongType(songType);
  }

  private getTagVariant(tag: MappedTag, tagsToAssign: MinimalTag[]): string {
    return getTagVariant(tag, tagsToAssign);
  }

  private getTagIcon(tag: MappedTag, tagsToAssign: MinimalTag[]): string[] {
    return getTagIconForTagAssignationButton(tag, tagsToAssign);
  }

  private toggleTagAssignation(
    tag: MappedTag,
    video: EntryWithVideosAndVisibility
  ): void {
    toggleTagAssignation(tag, video);
  }

  // api methods
  async fetch(newStartOffset: number, newPage: number): Promise<void> {
    this.fetching = true;
    localStorage.setItem("max_results", this.maxResults.toString());
    localStorage.setItem("start_offset", newStartOffset.toString());
    localStorage.setItem("order_by", this.orderingCondition);
    try {
      let videos: EntryWithVideosAndVisibility[] = [];
      this.distinctSongCount = 0;
      let end = false;
      let response;
      this.pageToJump = newPage;
      while (this.distinctSongCount < this.maxResults && !end) {
        response = await api.fetchVideosFromDb({
          startOffset: newStartOffset + this.distinctSongCount,
          maxResults: 10,
          orderBy: this.orderingCondition
        });
        end = response.items.length < 10;
        let videos_temp = response.items.map(entry => {
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
        });

        if (this.distinctSongCount > 0) {
          const overlap = videos_temp.find(
            video => video.song.id == videos[videos.length - 1].song.id
          );
          if (overlap != undefined) {
            videos_temp.splice(0, videos_temp.indexOf(overlap) + 1);
          }
        }
        this.distinctSongCount += videos_temp.length;
        videos = videos.concat(videos_temp);

        this.totalVideoCount = response.totalCount;
      }
      videos.splice(this.maxResults);
      this.videos = videos;
      this.postProcessVideos();
      this.filterVideos();
      this.distinctSongCount = this.maxResults;
      this.page = newStartOffset / this.maxResults + 1;
      this.numOfPages = this.totalVideoCount / this.maxResults + 1;
      this.startOffset = newStartOffset;
    } catch (err) {
      this.processError(err);
    } finally {
      this.maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
      this.fetching = false;
      this.page = newPage;
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

  private loadPage(pgNum: number): void {
    this.fetch((pgNum - 1) * this.maxResults, pgNum);
  }

  // error handling
  private processError(err: any): void {
    this.$bvToast.show(getUniqueElementId("error_", this.thisMode.toString()));
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
    let start_offset = localStorage.getItem("start_offset");
    if (start_offset != null) {
      this.startOffset = parseInt(start_offset);
    }
    let order_by = localStorage.getItem("order_by");
    if (order_by != null) {
      this.orderingCondition = order_by;
    }
    if (max_results != null && start_offset != null && order_by != null) {
      this.sessionLocked = true;
    }
  }

  private unlockSession(): void {
    this.sessionLocked = false;
  }
}
</script>
