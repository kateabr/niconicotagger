<template>
  <b-toast
    :id="'error_' + thisMode"
    :title="alertStatusText"
    no-auto-hide
    variant="danger"
    class="m-0 rounded-0 align-self-center"
    toaster="b-toaster-top-center"
  >
    <span v-html="alertMessage"></span>
  </b-toast>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component({ components: {} })
export default class extends Vue {
  @Prop()
  private readonly thisMode!: string;

  @Prop()
  private readonly alertMessage!: string;

  @Prop()
  private readonly alertStatusText!: string;
}
</script>
