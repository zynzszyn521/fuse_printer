#include "include/fuse_printer/fuse_printer_plugin_c_api.h"

#include <flutter/plugin_registrar_windows.h>

#include "fuse_printer_plugin.h"

void FusePrinterPluginCApiRegisterWithRegistrar(
    FlutterDesktopPluginRegistrarRef registrar) {
  fuse_printer::FusePrinterPlugin::RegisterWithRegistrar(
      flutter::PluginRegistrarManager::GetInstance()
          ->GetRegistrar<flutter::PluginRegistrarWindows>(registrar));
}
