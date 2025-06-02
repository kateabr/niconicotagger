<template>
  <b-row>
    <error-message
      :alert-status-text="alertStatusText"
      :alert-message="alertMessage"
      :this-mode="apiType"
    />
    <div
      class="py-lg-3 px-lg-4 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
    >
      <b-row>
        <b-col class="my-auto">
          <b-input-group>
            <template #prepend>
              <b-input-group-text>{{ queryBase }}</b-input-group-text>
            </template>
            <b-input v-model="userQuery" placeholder="Other query params" />
          </b-input-group>
        </b-col>
      </b-row>
      <b-row>
        <b-col class="my-auto">
          <b-button
            class="mt-2"
            block
            variant="primary"
            :href="queryBase + userQuery"
            target="_blank"
            :disabled="defaultDisableCondition()"
            >Preview
            <font-awesome-icon class="ml-1" icon="fas fa-external-link" />
          </b-button>
        </b-col>
        <b-col class="my-auto">
          <b-button
            class="mt-2"
            block
            variant="primary"
            :disabled="defaultDisableCondition()"
            @click="fetch()"
            ><span v-if="!songsInfoLoaded() && !fetching">Load</span>
            <b-spinner v-else-if="fetching" small />
            <span v-else>Reload</span></b-button
          >
        </b-col>
        <b-col class="my-auto">
          <b-button
            v-b-toggle="'cheatsheet'"
            class="mt-2"
            block
            variant="primary"
            :disabled="defaultDisableCondition()"
          >
            <font-awesome-icon class="mr-sm-1" icon="fas fa-angle-down" />
            About
          </b-button>
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <b-collapse :id="'cheatsheet'" v-model="showCheatsheet" class="mt-2">
            <b-card class="text-left">
              <b-card-title>About</b-card-title>
              <b-card-text style="color: black"
                >This mode allows browsing and filtering songs in the same
                flexible way as the
                <b-link :href="clientType + '/Search?searchType=Song'"
                  >songs
                </b-link>
                and
                <b-link :href="clientType + '/Search?searchType=Artist'"
                  >artists
                </b-link>
                pages do, except that the search query needs to be written
                manually.<br />
                Most common query segments are listed below, everything else can
                be extracted from the database search page's address bar or
                VocaDB's source code.<br />
                Clicking the "Preview" button will open a new tab and show you
                the results you will get with your current query, so you can
                debug it until it does what you need.
                <div class="alert-info rounded p-2 mt-2">
                  <b-icon-exclamation-circle class="mr-1" />Remember that tag
                  removal can't be undone, please be cautious & conscious :)
                </div>
              </b-card-text>
              <b-card-title>Basic query cheatsheet</b-card-title>
              <b-table-simple small>
                <tbody>
                  <tr>
                    <td class="text-monospace">start</td>
                    <td class=""><span class="text-monospace">number</span></td>
                    <td>
                      How many results to skip in the beginning. Defaults to
                      <span class="text-monospace">0</span>.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">maxResults</td>
                    <td class="text-monospace">
                      <span class="text-monospace">number</span>
                    </td>
                    <td>
                      How many results you want in your batch, defaults to
                      <span class="text-monospace">10</span>.
                      <div class="alert-dark rounded p-2 my-1">
                        <b-icon-arrow-right-circle class="mr-1" />Can be as big
                        as needed, but the page will freeze until all items are
                        processed (reload to abort).
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">sort</td>
                    <td class="text-monospace">
                      Name / AdditionDate / FavoritedTimes / RatingScore
                    </td>
                    <td>
                      Ordering condition. Can only use one, defaults to
                      <span class="text-monospace">None</span>.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">lang</td>
                    <td class="text-monospace">Japanese / Romaji / English</td>
                    <td>
                      Name language. Defaults to
                      <span class="text-monospace">Default</span>.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">tagId[]</td>
                    <td class="text-monospace">number</td>
                    <td>
                      Load only entries tagged with a specific tag.
                      <div class="alert-dark rounded p-2 my-1">
                        <b-icon-arrow-right-circle class="mr-1" />
                        Having several of these creates an intersection, not a
                        union.
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">excludedTags[]</td>
                    <td class="text-monospace">number</td>
                    <td>
                      Filter out entries tagged with one or more specific tags.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">childTags</td>
                    <td class="text-monospace">true / false</td>
                    <td>
                      Also load entries tagged by a chosen tag's descendants.
                      Defaults to <span class="text-monospace">false</span>.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">status</td>
                    <td class="text-monospace">
                      Draft / Finished / Approved / Locked
                    </td>
                    <td>
                      Status restriction, off by default. Can only use one.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      songTypes
                      <b-badge class="ml-1" variant="primary">songs </b-badge>
                    </td>
                    <td class="text-monospace">
                      Original, Remaster, Remix, Cover, Instrumental, Mashup,
                      MusicPV, DramaPV, Other
                    </td>
                    <td>
                      Song type restriction, defaults to
                      <span class="text-monospace">Unspecified</span> (no
                      restrictions). Separate with comma.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      artistId[]
                      <b-badge class="ml-1" variant="primary">songs </b-badge>
                    </td>
                    <td class="text-monospace">number</td>
                    <td>
                      Load only songs by a select artist.
                      <div class="alert-dark rounded p-2 my-1">
                        <b-icon-arrow-right-circle class="mr-1" />
                        Having several of these creates an intersection, not a
                        union.
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      childVoicebanks
                      <b-badge class="ml-1" variant="primary">songs</b-badge>
                    </td>
                    <td class="text-monospace">true / false</td>
                    <td>
                      Include songs that feature derived voicebanks (appends and
                      equivalents). Defaults to
                      <span class="text-monospace">false</span>.
                      <div class="alert-dark rounded p-2 my-1">
                        <b-icon-arrow-right-circle class="mr-1" />
                        Will only work for derivatives that have an artist
                        entry.
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      minScore
                      <b-badge class="ml-1" variant="primary">songs </b-badge>
                    </td>
                    <td class="text-monospace">number</td>
                    <td>
                      Rating threshold, defaults to
                      <span class="text-monospace">0</span>.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      dateYear, dateMonth, dateDay
                      <b-badge class="ml-1" variant="primary">songs </b-badge>
                    </td>
                    <td class="text-monospace">numbers</td>
                    <td>
                      Specific publish date. Can be used together or each on
                      their own.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      parentVersionId
                      <b-badge class="ml-1" variant="primary">songs </b-badge>
                    </td>
                    <td class="text-monospace">number</td>
                    <td>Id of the songs' original version.</td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      minLength, maxLength
                      <b-badge class="ml-1" variant="primary">songs </b-badge>
                    </td>
                    <td class="text-monospace">number</td>
                    <td>Length restriction (in seconds).</td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      eventId
                      <b-badge class="ml-1" variant="primary">songs </b-badge>
                    </td>
                    <td class="text-monospace">number</td>
                    <td>Id of the songs' release event.</td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      onlyWithPVs
                      <b-badge class="ml-1" variant="primary">songs </b-badge>
                    </td>
                    <td class="text-monospace">true / false</td>
                    <td>
                      Defaults to <span class="text-monospace">false.</span>
                      <div class="alert-dark rounded p-2 my-1">
                        <b-icon-arrow-right-circle class="mr-1" />Does not apply
                        to disabled PVs. Songs that have all qualifying PVs
                        disabled will be filtered out.
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      artistTypes
                      <b-badge class="ml-1" variant="success">artists </b-badge>
                    </td>
                    <td class="text-monospace">
                      Circle, Label, Producer, Animator, Illustrator, Lyricist,
                      Vocaloid, UTAU, CeVIO, OtherVoiceSynthesizer,
                      OtherVocalist, OtherGroup, SynthesizerV, CoverArtist
                    </td>
                    <td>
                      Artist type restriction, defaults to
                      <span class="text-monospace">Unspecified</span> (no
                      restrictions). Separate with comma.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      followedByUserId
                      <b-badge class="ml-1" variant="success">artists </b-badge>
                    </td>
                    <td class="text-monospace">number</td>
                    <td>Only load artists followed by a specific user.</td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      getTotalCount
                      <b-badge class="ml-1" variant="dark">not used </b-badge>
                    </td>
                    <td class="text-monospace">-</td>
                    <td>
                      Boolean value that indicates whether to return the total
                      number of matches. Hardcoded to
                      <span class="text-monospace">true</span>.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      fields
                      <b-badge class="ml-1" variant="dark">not used </b-badge>
                    </td>
                    <td class="text-monospace">-</td>
                    <td>
                      This parameter specifies which extra fields you would like
                      to get along with the basic entry. Here it is already set
                      to <span class="text-monospace">Tags</span>, so its
                      subsequent occurrences will be ignored.
                    </td>
                  </tr>
                </tbody>
              </b-table-simple>
              <b-card-title>Advanced filters</b-card-title>
              <b-card-text style="color: black"
                >These allow for a more fine-tuned search but also take more
                time to process.<br />
                <span style="text-decoration: underline"
                  >Please note that this feature does not come out of the box,
                  so it is possible that not every combination of conditions is
                  fully implemented and this is not an error.</span
                ><br />
                <p>
                  Each advanced filter parameter has the following structure:
                </p></b-card-text
              >
              <blockquote style="color: black" class="ml-3 text-monospace">
                advancedFilters[number][type / param / negate]=value
              </blockquote>
              <b-card-text style="color: black"
                >Here are some filter types:
                <ul>
                  <li>
                    ArtistType
                    <b-badge class="ml-1" variant="primary">songs </b-badge>
                  </li>
                  <li>
                    HasAlbum
                    <b-badge class="ml-1" variant="primary">songs </b-badge>
                  </li>
                  <li>
                    HasMedia
                    <b-badge class="ml-1" variant="primary">songs </b-badge>
                  </li>
                  <li>
                    HasOriginalMedia
                    <b-badge class="ml-1" variant="primary">songs </b-badge>
                  </li>
                  <li>
                    HasPublishDate
                    <b-badge class="ml-1" variant="primary">songs </b-badge>
                  </li>
                  <li>
                    Lyrics
                    <b-badge class="ml-1" variant="primary">songs </b-badge>
                  </li>
                  <li>
                    LyricsContent
                    <b-badge class="ml-1" variant="primary">songs </b-badge>
                  </li>
                  <li>
                    WebLink
                    <b-badge class="ml-1" variant="primary">songs </b-badge>
                  </li>
                  <li>
                    VoiceProvider
                    <b-badge class="ml-1" variant="success">artists </b-badge>
                  </li>
                  <li>
                    RootVoicebank
                    <b-badge class="ml-1" variant="success">artists </b-badge>
                  </li>
                  <li>
                    HasUserAccount
                    <b-badge class="ml-1" variant="success">artists </b-badge>
                  </li>
                </ul>
                <span class="text-monospace">Filter param</span> is the value
                you need: you can specify one or several exact values or use a
                wildcard (<span class="text-monospace">*</span>) to select all
                possible values. <span class="text-monospace">Negate</span> is
                used to invert the condition.
              </b-card-text>
              <b-card-text style="color: black"
                ><p>
                  <strong>Example:</strong> Filter for all songs that do not
                  feature UTAU and have lyrics in any language
                </p>
                <blockquote style="color: black" class="ml-3 text-monospace">
                  advancedFilters[0][filterType]=ArtistType&advancedFilters[0][negate]=true&advancedFilters[0][param]=UTAU&advancedFilters[1][filterType]=Lyrics&advancedFilters[1][param]=*
                </blockquote>
              </b-card-text>
            </b-card>
          </b-collapse>
        </b-col>
      </b-row>
      <b-row v-if="songsInfoLoaded()" cols="12" class="mt-2">
        <b-col cols="12" class="my-auto mx-auto">
          <b-card class="text-left" title="Tags to remove from all entries">
            <b-card-text v-if="tagsToMassRemove.length > 0">
              <b-button
                v-for="(tag, tagKey) in tagsToMassRemove"
                :key="tagKey"
                class="m-1"
                :disabled="defaultDisableCondition()"
                size="sm"
                variant="danger"
                @click="unstageMassRemoval(tag)"
              >
                <font-awesome-icon icon="fa-solid fa-xmark" class="mr-1" />
                {{ tag.name }}
              </b-button>
            </b-card-text>
            <b-card-text v-else class="text-muted">None selected</b-card-text>
            <vue-bootstrap-typeahead
              v-model="tempTagName"
              size="sm"
              prepend="Add a tag"
              :serializer="tag => tag.name"
              :data="tagPool"
              @hit="stageMassRemoval($event)"
            />
          </b-card>
        </b-col>
      </b-row>
    </div>
    <div class="py-lg-3 px-lg-4 col-lg-12 text-left m-auto">
      <b-row
        v-if="totalVideoCount >= entries.length && songsInfoLoaded()"
        cols="12"
      >
        <b-col
          >Query yielded {{ totalVideoCount }} results (displaying
          {{ entries.length }}):
        </b-col>
      </b-row>
      <b-row v-else-if="songsInfoLoaded() && entries.length === 0"
        >Query yielded no results.
      </b-row>
    </div>
    <b-table-simple
      v-if="songsInfoLoaded() && isActiveMode()"
      hover
      class="mt-1 col-lg-12"
    >
      <b-thead>
        <b-th class="col-3 align-middle">Entry</b-th>
        <b-th class="col-9 align-middle">Tags</b-th>
      </b-thead>
      <b-tbody v-if="entries.length > 0">
        <tr v-for="entry in entries" :key="entry.id">
          <td>
            <title-displayer
              :elem-type="apiType"
              :elem-name="entry.name"
              :elem-link="getEntryUrl(entry.id)"
              :elem-type-abbr="getShortenedType(entry)"
              :elem-type-color="getTypeColorForDisplay(entry)"
              :artist-string="apiType === 'songs' ? entry.artistString : ''"
            />
          </td>
          <td>
            <span v-for="(tag, tagKey) in entry.tags" :key="tagKey">
              <b-button
                size="sm"
                :variant="getTagVariant(entry, tag.id)"
                class="m-1"
                :disabled="defaultDisableCondition()"
                @click="toggleIndividualTagRemoval(entry, tag)"
              >
                <font-awesome-icon
                  :icon="getTagIcon(tag.id)"
                  class="sm mr-sm-1"
                />{{ tag.name }}</b-button
              >
            </span>
            <entry-error-report :error-report="entry.errorReport" />
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
        <b-th class="col-3 align-middle">Entry</b-th>
        <b-th class="col-9 align-middle">Tags</b-th>
      </b-tfoot>
    </b-table-simple>
    <b-row
      v-if="songsInfoLoaded()"
      class="mt-lg-1 col-lg-12 mb-5 text-center mx-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
    >
      <b-col class="col-lg-3 m-auto">
        <b-button
          block
          variant="primary"
          :disabled="countChecked() === 0 || executing"
          @click="removeTags"
        >
          <div v-if="executing">
            <b-spinner small class="mr-1"></b-spinner>
            Removing tags...
          </div>
          <div v-else>
            Remove tags from songs ({{ countChecked() }} selected)
          </div>
        </b-button>
      </b-col>
    </b-row>
  </b-row>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import {
  getErrorData,
  getArtistTypeColorForDisplay,
  getBaseUrl,
  getClientType,
  getShortenedArtistType,
  getShortenedSongType,
  getSongTypeColorForDisplay,
  getUniqueElementId,
  getVocaDBArtistUrl,
  getVocaDBSongUrl
} from "@/utils";
import { api } from "@/backend";
import NicoEmbed from "@/components/NicoEmbed.vue";
import ErrorMessage from "@/components/ErrorMessage.vue";
import TitleDisplayer from "@/components/TitleDisplayer.vue";
import { AxiosError, AxiosResponse } from "axios";
import {
  QueryConsoleArtistItem,
  QueryConsoleSongItem
} from "@/backend/dto/response/queryConsoleResponse";
import EntryErrorReport from "@/components/EntryErrorReport.vue";
import {
  ClientType,
  QueryConsoleApiType,
  SongType
} from "@/backend/dto/enumeration";
import { UpdateErrorReport, VocaDbTag } from "@/backend/dto/lowerLevelStruct";

@Component({
  components: {
    EntryErrorReport,
    TitleDisplayer,
    NicoEmbed,
    ErrorMessage
  }
})
export default class extends Vue {
  @Prop()
  private readonly currentMode!: string;

  @Prop()
  private readonly userQueryProp!: string;

  @Prop()
  private readonly apiType!: QueryConsoleApiType;

  // main variables
  private entries: (QueryConsoleArtistItem | QueryConsoleSongItem)[] = [];
  private tagPool: VocaDbTag[] = [];
  private tagIdsForRemoval: number[] = [];
  private tagsToMassRemove: VocaDbTag[] = [];
  private tempTagName: string = "";

  // api variables
  private clientType: ClientType = getClientType();
  private queryBase: string =
    "/api/" + this.apiType + "?fields=Tags&getTotalCount=true&";
  private userQuery: string = "maxResults=10";

  // interface variables
  private executing: boolean = false;
  private fetching: boolean = false;
  private totalVideoCount: number = 0;
  private loaded: boolean = false;
  private showCheatsheet: boolean = false;

  // error handling
  private alertStatusText = "";
  private alertMessage: string = "";

  //proxy methods
  private getTagVariant(
    entry: QueryConsoleArtistItem | QueryConsoleSongItem,
    tagId: number
  ): string {
    if (entry.tagsToRemove.map(entryTag => entryTag.id).includes(tagId)) {
      return "danger";
    } else {
      return "outline-success";
    }
  }

  private getTagIcon(tagUsageId: number): string[] {
    if (this.tagIdsForRemoval.find(t => t == tagUsageId) != undefined) {
      return ["fas", "fa-xmark"];
    } else {
      return ["fas", "fa-check"];
    }
  }

  private getShortenedType(
    entry: QueryConsoleSongItem | QueryConsoleArtistItem
  ): string {
    if (this.apiType == "songs") {
      let typeString = (entry as QueryConsoleSongItem).type;
      return getShortenedSongType(SongType[typeString]);
    } else if (this.apiType == "artists") {
      let typeString = (entry as QueryConsoleArtistItem).type;
      return getShortenedArtistType(typeString);
    } else {
      return "Invalid removal currentMode: " + this.apiType;
    }
  }

  private getEntryUrl(id: number): string {
    if (this.apiType == "songs") {
      return getVocaDBSongUrl(this.clientType, id);
    } else if (this.apiType == "artists") {
      return getVocaDBArtistUrl(this.clientType, id);
    } else {
      return "Invalid api type: " + this.apiType;
    }
  }

  private getTypeColorForDisplay(
    entry: QueryConsoleSongItem | QueryConsoleArtistItem
  ): string {
    if (this.apiType == "songs") {
      return getSongTypeColorForDisplay(
        SongType[(entry as QueryConsoleSongItem).type]
      );
    } else if (this.apiType == "artists") {
      return getArtistTypeColorForDisplay(
        (entry as QueryConsoleArtistItem).type
      );
    } else {
      return "Invalid api type: " + this.apiType;
    }
  }

  // interface methods
  private isActiveMode(): boolean {
    return this.currentMode == this.apiType;
  }

  private defaultDisableCondition(): boolean {
    return this.fetching || this.executing;
  }

  private songsInfoLoaded(): boolean {
    return this.loaded;
  }

  private countChecked(): number {
    return this.entries.filter(entry => entry.tagsToRemove.length > 0).length;
  }

  private toggleIndividualTagRemoval(
    entry: QueryConsoleArtistItem | QueryConsoleSongItem,
    tag: VocaDbTag
  ): void {
    if (entry.tagsToRemove.map(entryTag => entryTag.id).includes(tag.id)) {
      entry.tagsToRemove = entry.tagsToRemove.filter(
        stagedTag => stagedTag.id != tag.id
      );
    } else {
      entry.tagsToRemove.push(tag);
    }
  }

  // for tag autocomplete
  private stageMassRemoval(tag: VocaDbTag): void {
    this.tagsToMassRemove.push(tag);
    for (const entry of this.entries) {
      if (entry.tags.some(entryTag => entryTag.id == tag.id)) {
        entry.tagsToRemove.push(tag);
      }
    }
  }

  private unstageMassRemoval(tag: VocaDbTag): void {
    for (const entry of this.entries) {
      entry.tagsToRemove = entry.tagsToRemove.filter(
        entryTag => entryTag.id != tag.id
      );
    }
    this.tagsToMassRemove = this.tagsToMassRemove.filter(
      stagedTag => stagedTag.id != tag.id
    );
  }

  private updateUrl(): void {
    this.$router
      .push({
        name: "console-full",
        params: { browseMode: this.apiType, userQuery: this.userQuery }
      })
      .catch(err => {
        return false;
      });
  }

  // api methods
  async fetch(): Promise<void> {
    this.updateUrl();
    this.tagsToMassRemove = [];
    this.fetching = true;
    this.showCheatsheet = false;
    try {
      let response = await api.getDataByCustomQuery({
        apiType: this.apiType,
        query: this.userQuery,
        clientType: this.clientType
      });
      this.entries = response.items.map(item => {
        switch (this.apiType) {
          case "artists":
            return {
              id: item.id,
              name: item.name,
              tags: item.tags,
              type: (item as QueryConsoleArtistItem).type,
              tagsToRemove: [],
              errorReport: null
            };
          case "songs":
            return {
              id: item.id,
              name: item.name,
              tags: item.tags,
              type: (item as QueryConsoleSongItem).type,
              artistString: (item as QueryConsoleSongItem).artistString,
              tagsToRemove: [],
              errorReport: null
            };
        }
      });
      this.tagPool = response.tagPool;
      this.totalVideoCount = response.totalCount;
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      this.loaded = true;
      this.fetching = false;
    }
  }

  private async removeTags(): Promise<void> {
    this.executing = true;
    this.showCheatsheet = false;
    try {
      const entriesToUpdate = this.entries.filter(
        entry => entry.tagsToRemove.length > 0
      );
      for (const entryToUpdate of entriesToUpdate) {
        await this.removeSingle(entryToUpdate);
      }
      const newTagPool = this.entries
        .map(entry => entry.tags)
        .flat() as VocaDbTag[];
      this.tagPool = this.tagPool.filter(poolTag =>
        newTagPool.some(newPoolTag => poolTag.id == newPoolTag.id)
      );
      this.tagsToMassRemove = [];
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      this.executing = false;
    }
  }

  private async removeSingle(
    entry: QueryConsoleArtistItem | QueryConsoleSongItem
  ): Promise<void> {
    let error = await api.removeTagUsages({
      request: {
        apiType: this.apiType,
        entryId: entry.id,
        tags: entry.tagsToRemove
      },
      clientType: this.clientType
    });

    if (error.entryId != entry.id) {
      entry.tags = entry.tags.filter(
        tag => !entry.tagsToRemove.map(entryTag => entryTag.id).includes(tag.id)
      );
      entry.tagsToRemove = [];
      entry.errorReport = null;
    } else {
      entry.errorReport = error as UpdateErrorReport;
    }
  }

  // error handling
  private processError(response: AxiosResponse | undefined): void {
    const errorData = getErrorData(response);
    this.alertMessage = errorData.message;
    this.alertStatusText = errorData.statusText;
    this.$bvToast.show(getUniqueElementId("error_", this.apiType.toString()));
  }

  // session
  created(): void {
    if (this.clientType != ClientType.UNKNOWN) {
      this.queryBase = getBaseUrl(this.clientType) + this.queryBase;
    }
  }

  // fill userQuery name from address params
  mounted(): void {
    let userQuery = this.$route.params["userQuery"];
    if (userQuery != undefined) {
      this.userQuery = userQuery;
    }
  }
}
</script>
