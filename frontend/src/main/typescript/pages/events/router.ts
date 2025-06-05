import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";
import { serviceName } from "@/constants";
import { BrowseMode, defaultMode } from "@/pages/events/utils";

Vue.use(VueRouter);

const routes: RouteConfig[] = [
  {
    path: "/",
    name: "events",
    props: {
      browseMode: defaultMode
    },
    component: () => import("@/pages/events/views/Events.vue")
  },
  {
    path: "/:browseMode",
    name: "events-mode",
    props: true,
    component: () => import("@/pages/events/views/Events.vue")
  },
  {
    path: "/:browseMode/:targName",
    name: "events-full",
    props: true,
    component: () => import("@/pages/events/views/Events.vue")
  }
];

export const router = new VueRouter({
  base: "/events",
  mode: "history",
  routes: routes
});

router.beforeEach((to, from, next) => {
  const mode: BrowseMode = (to.params["browseMode"] as BrowseMode) ?? defaultMode;
  const eventName = to.params["targName"] ?? "Events";
  switch (mode) {
    case "event-schedule":
      document.title = `${serviceName} | Event schedule`;
      break;
    case "vocadb":
      document.title = `${serviceName} | ${eventName} (VocaDB)`;
      break;
    case "nicovideo":
      document.title = `${serviceName} | ${eventName} (NND)`;
      break;
    default:
      document.title = `${serviceName} | ${eventName} (${mode})`;
      break;
  }
  next();
});
