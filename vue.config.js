// eslint-disable-next-line @typescript-eslint/no-var-requires
const path = require("path");

module.exports = {
  pages: {
    home: {
      title: "NicoNicoTagger",
      entry: "src/main/typescript/pages/home/main.ts"
    },
    login: {
      title: "NicoNicoTagger",
      entry: "src/main/typescript/pages/login/main.ts"
    }
  },

  configureWebpack: {
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "src/main/typescript")
      }
    }
  },

  outputDir: "target/spa",
  lintOnSave: false,

  css: {
    sourceMap: true
  },

  devServer: {
    proxy: "http://localhost:8080"
  }
};
