Magnet Portable Magnet Texture Pack

Namespace:
- The plugin uses magnit:portable_magnet.
- Do not rename assets/magnit to assets/magnet, or the item can render as missing texture.

Paper/Leaf 1.21.x item_model component, when available:
- Item model key: magnit:portable_magnet
- Item definition: assets/magnit/items/portable_magnet.json

CustomModelData fallback:
- Minecraft 1.17+: base item AMETHYST_SHARD
- Minecraft 1.16.5: base item COMPASS
- CustomModelData value: 9001001
- Modern override: assets/minecraft/models/item/amethyst_shard.json
- 1.16.5 packs need an equivalent compass model override

Server setup:
- resource-pack-prompt in server.properties must be a JSON text component, not plain text.
