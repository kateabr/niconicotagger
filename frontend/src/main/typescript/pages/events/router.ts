import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";
import { serviceName } from "@/constants";

Vue.use(VueRouter);

const routes: RouteConfig[] = [
  {
    path: "/",
    name: "events",
    props: {
      browseMode: "nicovideo"
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
  const mode = to.params["browseMode"];
  if (mode == "event-schedule") {
    document.title = `${serviceName} | Event schedule`;
    next();
    return;
  }
  const eventName = to.params["targName"] ?? "Events";
  switch (mode) {
    case "vocadb":
      document.title = `${serviceName} | ${eventName} (VocaDB)`;
      break;
    case "nicovideo":
    case undefined:
      document.title = `${serviceName} | ${eventName} (NND)`;
      break;
    default:
      document.title = `${serviceName} | ${eventName} (${mode})`;
      break;
  }
  next();
});
