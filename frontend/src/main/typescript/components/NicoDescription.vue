<template>
  <span>
    <span v-if="description !== null">
      <b-button
        v-b-toggle="contentId + '_description'"
        variant="link"
        size="sm"
        class="py-0"
        ><font-awesome-icon
          class="mr-sm-1"
          icon="fas fa-angle-down"
        />Description</b-button
      >
      <b-collapse :id="contentId + '_description'" class="collapsed mt-2">
        <b-card v-cloak>
          <div v-if="publisher !== null" class="mb-2 text-secondary">
            <b-badge
              v-if="publisher.publisherNickname !== null"
              v-clipboard:copy="publisher.publisherNickname"
              variant="primary"
              class="m-sm-1"
              href="#"
              ><font-awesome-icon class="mr-sm-1" icon="fas fa-user" />{{
                publisher.publisherNickname
              }}</b-badge
            ><span class="ml-1"
              >(<b-link
                target="_blank"
                :href="'https://www.nicovideo.jp/user/' + publisher.publisherId"
                >user/{{ publisher.publisherId }}</b-link
              >)</span
            >
            <div v-html="description" />
          </div>
        </b-card>
      </b-collapse>
    </span>
    <span v-else class="text-secondary">
      <small>Could not extract description</small>
      <div v-if="publisher !== null" class="small">
        <b-badge
          v-if="publisher.publisherNickname !== null"
          v-clipboard:copy="publisher.publisherNickname"
          variant="primary"
          class="m-sm-1"
          href="#"
          ><font-awesome-icon class="mr-sm-1" icon="fas fa-user" />{{
            publisher.publisherNickname
          }}</b-badge
        >
        (<b-link
          target="_blank"
          :href="'https://www.nicovideo.jp/user/' + publisher.publisherId"
          >user/{{ publisher.publisherId }}</b-link
        >)
      </div>
    </span>
  </span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { NicoPublisherWithoutEntry } from "@/backend/dto";

@Component({ components: {} })
export default class extends Vue {
  @Prop()
  private readonly contentId!: string;
  @Prop()
  private readonly description!: string | null;
  @Prop()
  private readonly forceCollapse!: boolean;
  @Prop()
  private readonly publisher!: NicoPublisherWithoutEntry | null;
}
</script>
