<template>
  <b-toast
    :id="'error_' + thisMode"
    title="Error"
    no-auto-hide
    variant="danger"
    class="m-0 rounded-0"
    toaster="toaster-events"
  >
    <span v-if="alertCode !== 401">
      {{ alertMessage }}
    </span>
    <span v-else>
      Access token has expired.
      <b-link to="login" target="_blank">
        Relogin
        <font-awesome-icon class="ml-0" icon="fas fa-external-link" />
      </b-link>
      and try again
    </span>
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
  private readonly alertCode!: number;

  @Prop()
  private readonly alertMessage!: string;
}
</script>
