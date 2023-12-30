import 'dart:async';

import 'package:flutter/services.dart';

class CunningDocumentScanner {
  static const MethodChannel _channel = MethodChannel('cunning_document_scanner');

  /// Call this to start get Picture workflow.
  static Future<List<String>?> getPictures({required List<String> imagesPath, required bool crop}) async {
    // Map<Permission, PermissionStatus> statuses = await [
    //   Permission.camera,
    //   Permission.storage,
    //   Permission.photos,
    // ].request();
    // if (statuses.containsValue(PermissionStatus.denied) || statuses.containsValue(PermissionStatus.permanentlyDenied)) {
    //   throw Exception("Permission not granted");
    // }

    // Check permission from flutter level

    final List<dynamic>? pictures = await _channel.invokeMethod(
      'getPictures',
      {
        "crop": crop,
        "imagesPath": imagesPath,
      },
    );
    return pictures?.map((e) => e as String).toList();
  }
}
