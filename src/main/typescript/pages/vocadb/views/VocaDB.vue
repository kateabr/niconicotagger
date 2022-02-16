<template>
  <div>
    <div class="flex-fill fixed-top mb-3">
      <b-progress
        height="5px"
        :value="distinct_song_count"
        :max="maxResults"
        :precision="0"
        :animated="fetching"
        striped
      />
    </div>

    <b-row class="col-12">
      <b-col>
        <div style="display: flex; align-items: center">
          <b-container class="col-lg-11">
            <b-row>
              <b-toaster
                class="b-toaster-top-center"
                name="toaster-2"
              ></b-toaster>
              <b-row
                class="mt-lg-3 pt-lg-3 pb-lg-3 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
              >
                <b-col v-if="videos.length > 0" class="my-auto">
                  <b-dropdown
                    :disabled="defaultDisableCondition()"
                    block
                    :text="getResultNumberStr()"
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
                <b-col v-if="videos.length > 0" class="my-auto">
                  <b-dropdown
                    block
                    :disabled="defaultDisableCondition()"
                    :text="getOrderingCondition()"
                    variant="primary"
                  >
                    <b-dropdown-item
                      v-for="(key, value) in orderOptions"
                      :key="key"
                      :disabled="orderBy === value"
                      @click="setOrderBy(value)"
                    >
                      {{ orderOptions[value] }}
                    </b-dropdown-item>
                  </b-dropdown>
                </b-col>
                <b-col v-if="videos.length > 0" class="my-auto">
                  <template>
                    <b-input-group
                      inline
                      :state="pageState"
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
                          :state="pageState()"
                          @keydown.enter.native="
                            pageState()
                              ? fetch((pageToJump - 1) * maxResults, pageToJump)
                              : null
                          "
                        >
                        </b-form-input>
                      </template>
                      <template #append>
                        <b-button
                          style="width: 80px"
                          :variant="pageState() ? 'success' : 'danger'"
                          :disabled="defaultDisableCondition() || !pageState()"
                          @click="
                            fetch((pageToJump - 1) * maxResults, pageToJump)
                          "
                        >
                          <span v-if="fetching"><b-spinner small /></span>
                          <span v-else-if="pageToJump === page">Refresh</span>
                          <span v-else>Jump</span>
                        </b-button>
                      </template>
                    </b-input-group>
                  </template>
                </b-col>
                <b-col v-if="videos.length === 0" class="col-3 m-auto">
                  <b-button
                    variant="primary"
                    block
                    :disabled="defaultDisableCondition()"
                    @click="fetch(0, 1)"
                    ><span v-if="fetching"><b-spinner small /></span>
                    <span v-else>Load</span>
                  </b-button>
                </b-col>
                <b-col v-if="videos.length > 0" class="my-auto">
                  <b-button
                    variant="primary"
                    block
                    :pressed="showEntriesWithErrors"
                    :disabled="defaultDisableCondition()"
                    @click="toggleShowEntriesWithErrors"
                    >Entries with errors
                  </b-button>
                </b-col>
                <b-col v-if="videos.length > 0" class="my-auto">
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
              <b-row v-if="videos.length !== 0" class="col-12">
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
                          getSongTypeVariant(type.name)
                        "
                        @click="
                          type.show = !type.show;
                          filterVideos();
                        "
                        >{{ getTypeInfo(type.name) }}
                      </b-button>
                    </b-button-group>
                  </div>
                </b-col>
              </b-row>
              <b-row v-if="videos.length !== 0" class="col-12">
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
                      @change="pageClicked"
                    ></b-pagination>
                  </div>
                </template>
              </b-row>
              <b-table-simple
                v-if="videos.length > 0"
                hover
                class="mt-1 col-lg-12"
              >
                <b-thead>
                  <b-th>
                    <b-form-checkbox
                      class="invisible"
                      size="lg"
                    ></b-form-checkbox>
                  </b-th>
                  <b-th class="col-3 align-middle">Entry</b-th>
                  <b-th class="col-9 align-middle">Videos</b-th>
                </b-thead>
                <b-tbody
                  v-if="videos.filter(video => video.visible).length > 0"
                >
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
                      <a
                        target="_blank"
                        :href="getEntryUrl(video.song)"
                        v-html="video.song.name"
                      ></a>
                      <a target="_blank" :href="getEntryUrl(video.song)">
                        <b-badge
                          class="badge text-center ml-2"
                          :variant="getSongTypeVariant(video.song.songType)"
                        >
                          {{ getSongType(video.song.songType) }}
                        </b-badge>
                      </a>
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
                          <a
                            target="_blank"
                            :href="getVideoUrl(thumbnail.thumbnail.id)"
                            v-html="thumbnail.thumbnail.title"
                          ></a>
                          <div>
                            <b-badge
                              v-for="(nico_tag, key) in thumbnail.nicoTags"
                              :key="key"
                              v-clipboard:copy="nico_tag.name"
                              class="m-sm-1"
                              href="#"
                              :variant="nico_tag.variant"
                              >{{ nico_tag.name
                              }}<font-awesome-icon
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
                            <b-card
                              v-cloak
                              :id="'embed_' + thumbnail.thumbnail.id"
                              class="embed-responsive embed-responsive-16by9"
                            >
                              <iframe
                                v-if="thumbnail.expanded && !fetching"
                                class="embed-responsive-item"
                                allowfullscreen="allowfullscreen"
                                style="border: none"
                                :src="getEmbedAddr(thumbnail.thumbnail.id)"
                              ></iframe>
                            </b-card>
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
                              :disabled="
                                tag.assigned || defaultDisableCondition()
                              "
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
                        v-for="(
                          thumbnail, thumbnail_err_key
                        ) in video.thumbnailsErr"
                        :key="thumbnail_err_key"
                      >
                        <b-col>
                          <b-link
                            :to="getDeletedVideoAddr(thumbnail.id)"
                            target="_blank"
                            >{{ thumbnail.title }}</b-link
                          ><span
                            ><b-badge variant="danger" size="sm" class="ml-1">{{
                              thumbnail.code
                            }}</b-badge></span
                          >
                          <div>
                            <span
                              ><b-badge
                                v-if="!thumbnail.disabled"
                                variant="warning"
                                class="mr-1"
                              >
                                Needs to be disabled</b-badge
                              >
                            </span>
                          </div>
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
                  <b-th> </b-th>
                  <b-th class="col-3 align-middle">Entry</b-th>
                  <b-th class="col-9 align-middle">Videos</b-th>
                </b-tfoot>
              </b-table-simple>
              <b-row
                v-if="videos.length !== 0"
                class="mt-lg-1 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
              >
                <b-col class="col-lg-3 m-auto">
                  <b-button
                    block
                    variant="primary"
                    :disabled="
                      countChecked() === 0 || massAssigning || fetching
                    "
                    @click="assignMultiple"
                  >
                    <div v-if="massAssigning">
                      <b-spinner small class="mr-1"></b-spinner>
                      Assigning...
                    </div>
                    <div v-else>
                      Batch assign ({{ countChecked() }} selected)
                    </div>
                  </b-button>
                </b-col>
              </b-row>

              <b-row v-if="videos.length !== 0" class="col-12">
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
                      @change="pageClicked"
                    ></b-pagination>
                  </div>
                </template>
              </b-row>
              <b-toast
                id="error"
                title="Error"
                no-auto-hide
                variant="danger"
                class="m-0 rounded-0"
                toaster="toaster-2"
              >
                {{ alertMessage }}
              </b-toast>
            </b-row>
          </b-container>
          <b-row>
            <b-col>
              <b-link to="nicovideo" target="_blank">
                <b-button size="sm" variant="dark" class="fixed-top m-1" squared
                  >Toggle<br />mode</b-button
                >
              </b-link>
            </b-col>
          </b-row>
        </div>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component } from "vue-property-decorator";
import {
  MappedTag,
  MinimalTag,
  NicoVideoWithError,
  NicoVideoWithMappedTags,
  SongForApiContractSimplified
} from "@/backend/dto";
import { api } from "@/backend";

import VueClipboard from "vue-clipboard2";

Vue.use(VueClipboard);

@Component({ components: {} })
export default class extends Vue {
  private orderBy = "AdditionDate";
  private orderOptions = {
    PublishDate: "upload time",
    AdditionDate: "addition time",
    RatingScore: "user rating"
  };
  private startOffset: number = 0;
  private maxResults: number = 10;
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
  private videos: EntryWithVideosAndVisibility[] = [];
  private totalVideoCount: number = 0;
  private fetching: boolean = false;
  private showTable: boolean = false;
  private page: number = 1;
  private numOfPages: number = 1;
  private massAssigning: boolean = false;
  private alertMessage: string = "";
  private pageToJump: number = this.page;
  private maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
  private hideEntriesWithNoTags: boolean = false;
  private showEntriesWithErrors: boolean = true;
  private songTypeToTag = {
    Unspecified: [],
    Original: [6479],
    Remaster: [1519, 391, 371],
    Remix: [371, 74, 391],
    Cover: [74, 371],
    Instrumental: [208],
    MusicPV: [7378, 74],
    Mashup: [3392],
    DramaPV: [104],
    Other: []
  };
  private distinct_song_count: number = 0;

  async fetch(newStartOffset: number, newPage: number): Promise<void> {
    this.fetching = true;
    try {
      let videos: EntryWithVideosAndVisibility[] = [];
      this.distinct_song_count = 0;
      let end = false;
      while (this.distinct_song_count < this.maxResults && !end) {
        let response = await api.fetchVideosFromDb({
          startOffset: newStartOffset + this.distinct_song_count,
          maxResults: 10,
          orderBy: this.orderBy
        });
        let videos_temp = response.items.map(entry => {
          return {
            song: entry.song,
            toAssign: false,
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
            tagsToAssign: []
          };
        });
        end = videos_temp.length < 10;
        if (this.distinct_song_count > 0) {
          const overlap = videos_temp.find(
            video => video.song.id == videos[videos.length - 1].song.id
          );
          if (overlap != undefined) {
            videos_temp.splice(0, videos_temp.indexOf(overlap) + 1);
          }
        }
        this.distinct_song_count += videos_temp.length;
        videos = videos.concat(videos_temp);

        this.totalVideoCount = response.totalCount;
      }
      videos.splice(this.maxResults);
      this.videos = videos;
      this.postProcessVideos();
      this.filterVideos();
      this.distinct_song_count = 100;
      this.showTable = this.videos.length > 0;
      this.page = newStartOffset / this.maxResults + 1;
      this.numOfPages = this.totalVideoCount / this.maxResults + 1;
      this.startOffset = newStartOffset;
    } catch (err) {
      this.$bvToast.show("error");
      if (err.response == undefined) {
        this.alertMessage = err.message;
      } else {
        this.alertMessage = err.response.data.message;
      }
    } finally {
      this.maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
      this.fetching = false;
      this.pageToJump = newPage;
      this.page = newPage;
    }
  }

  getEntryUrl(songEntry: SongForApiContractSimplified): string {
    return "https://vocadb.net/S/" + songEntry.id;
  }

  getVideoUrl(video: string): string {
    return "https://nicovideo.jp/watch/" + video;
  }

  getResultNumberStr(): string {
    return "Entries per page: " + this.maxResults;
  }

  pageClicked(pgnum: number): void {
    this.fetch((pgnum - 1) * this.maxResults, pgnum);
  }

  setMaxResults(mxres: number): void {
    this.maxResults = mxres;
    this.fetch(0, 1);
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
        song.tagsToAssign.splice(0, song.tagsToAssign.length);
      }
    } finally {
      this.massAssigning = false;
    }
  }

  getButtonId(song: SongForApiContractSimplified): string {
    return "assign_" + song.id;
  }

  getCollapseId(videoId: string): string {
    return "collapse_" + videoId;
  }

  getEmbedAddr(videoId: string): string {
    return (
      "https://embed.nicovideo.jp/watch/" +
      videoId +
      "?noRelatedVideo=1&enablejsapi=0"
    );
  }

  getDeletedVideoAddr(videoId: string): string {
    return "https://nicolog.jp/watch/" + videoId;
  }

  countChecked(): number {
    return this.videos.filter(video => video.toAssign).length;
  }

  getSongType(typeString: string): string {
    if (typeString == "Unspecified") {
      return "?";
    } else if (typeString == "MusicPV") {
      return "PV";
    } else {
      return typeString[0];
    }
  }

  getSongTypeVariant(typeString: string): string {
    if (typeString == "Original" || typeString == "Remaster") {
      return "primary";
    } else if (
      typeString == "Remix" ||
      typeString == "Cover" ||
      typeString == "Mashup" ||
      typeString == "Other"
    ) {
      return "secondary";
    } else if (typeString == "Instrumental") {
      return "dark";
    } else if (typeString == "MusicPV" || typeString == "DramaPV") {
      return "success";
    } else {
      return "warning";
    }
  }

  private setOrderBy(value: string): void {
    this.orderBy = value;
    this.fetch(0, 1);
  }

  private filterVideos(): void {
    for (const video of this.videos) {
      let assignable_mapped_tags_cnt = 0;
      for (const thumbnailOk of video.thumbnailsOk) {
        assignable_mapped_tags_cnt += thumbnailOk.mappedTags.filter(
          tag => !tag.assigned
        ).length;
      }

      video.visible =
        ((this.hiddenTypes() == 0 ||
          !this.songTypes
            .filter(t => !t.show)
            .map(t => t.name)
            .includes(video.song.songType)) &&
          (!this.hideEntriesWithNoTags || assignable_mapped_tags_cnt > 0)) ||
        (this.showEntriesWithErrors && video.thumbnailsErr.length > 0);
    }
  }

  pageState(): boolean {
    return this.pageToJump > 0 && this.pageToJump <= this.maxPage;
  }

  private hiddenTypes(): number {
    return this.songTypes.filter(t => !t.show).length;
  }

  private getOrderingCondition(): string {
    return "Arrange by: " + this.orderOptions[this.orderBy];
  }

  private defaultDisableCondition(): boolean {
    return this.fetching || this.massAssigning;
  }

  private getTypeInfo(type: string): string {
    return (
      type +
      " (" +
      this.videos.filter(vid => vid.song != null && vid.song.songType == type)
        .length +
      ")"
    );
  }

  private getTagVariant(tag: MappedTag, tagsToAssign: MinimalTag[]): string {
    if (tag.assigned) {
      return "success";
    } else if (tagsToAssign.find(t => t.id == tag.tag.id) != undefined) {
      return "warning";
    } else {
      return "outline-success";
    }
  }

  private getTagIcon(tag: MappedTag, tagsToAssign: MinimalTag[]): string[] {
    if (tag.assigned) {
      return ["fas", "fa-check"];
    } else if (tagsToAssign.find(t => t.id == tag.tag.id) != undefined) {
      return ["fas", "fa-minus"];
    } else {
      return ["fas", "fa-plus"];
    }
  }

  private toggleTagAssignation(
    tag: MappedTag,
    video: EntryWithVideosAndVisibility
  ) {
    const assign =
      video.tagsToAssign.filter(t => t.id == tag.tag.id).length > 0;
    if (assign) {
      video.tagsToAssign = video.tagsToAssign.filter(t => t.id != tag.tag.id);
    } else {
      video.tagsToAssign.push(tag.tag);
    }
    for (const thumbnailOk of video.thumbnailsOk) {
      for (const mappedTag of thumbnailOk.mappedTags) {
        if (mappedTag.tag.id == tag.tag.id) {
          mappedTag.toAssign = !assign;
        }
      }
      video.toAssign = video.tagsToAssign.length > 0;
    }
  }

  private toggleShowEntriesWithErrors() {
    this.showEntriesWithErrors = !this.showEntriesWithErrors;
    this.filterVideos();
  }

  private toggleHideEntriesWithNoTags() {
    this.hideEntriesWithNoTags = !this.hideEntriesWithNoTags;
    this.filterVideos();
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
}

export interface EntryWithVideosAndVisibility {
  thumbnailsOk: NicoVideoWithMappedTags[];
  thumbnailsErr: NicoVideoWithError[];
  song: SongForApiContractSimplified;
  visible: boolean;
  toAssign: boolean;
  tagsToAssign: MinimalTag[];
}

export interface SongType {
  name: string;
  show: boolean;
}
</script>
