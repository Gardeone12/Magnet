Magnet Portable Magnet Texture Pack

Namespace:
- The plugin uses magnit:portable_magnet.
- Do not rename assets/magnit to assets/magnet, or the item can render as missing texture.

Paper/Leaf 1.21.11 item_model component:
- Item model key: magnit:portable_magnet
- Item definition: assets/magnit/items/portable_magnet.json

CustomModelData fallback:
- Base item: AMETHYST_SHARD
- CustomModelData float: 9001001
- Legacy override: assets/minecraft/models/item/amethyst_shard.json

Server setup:
- resource-pack-prompt in server.properties must be a JSON text component, not plain text.
