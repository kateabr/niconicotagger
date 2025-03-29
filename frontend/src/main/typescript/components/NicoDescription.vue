<template>
  <span>
    <b-button
      v-b-toggle="contentId + '_description'"
      variant="link"
      size="sm"
      class="py-0"
    >
      <font-awesome-icon
        class="mr-sm-1"
        icon="fas fa-angle-down"
      />Description</b-button
    >
    <b-collapse :id="contentId + '_description'" class="collapsed mt-2">
      <b-card v-cloak>
        <div
          v-if="publisher !== null && publisher.type !== 'DATABASE'"
          class="mb-2 text-secondary"
        >
          <b-badge
            v-if="publisher.name !== null"
            v-clipboard:copy="publisher.name"
            variant="primary"
            class="m-sm-1"
            href="#"
          >
            <font-awesome-icon class="mr-sm-1" icon="fas fa-user" />{{
              publisher.name
            }}
          </b-badge>
          <span class="ml-1">
            (<b-link target="_blank" :href="publisher.link">{{
              publisher.linkText
            }}</b-link
            >)
          </span>
        </div>
        <span>
          <div v-if="description !== null" v-html="description" />
          <div v-else class="text-secondary font-italic">
            Description unavailable
          </div>
        </span>
      </b-card>
    </b-collapse>
  </span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { PublisherInfo } from "@/backend/dto/lowerLevelStruct";

@Component({
  components: {}
})
export default class extends Vue {
  @Prop()
  private readonly contentId!: string;
  @Prop()
  private readonly description!: string | null;
  @Prop()
  private readonly publisher!: PublisherInfo | null;
}
</script>
