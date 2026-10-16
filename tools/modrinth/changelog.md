补丁版：修掉「死亡复活之后，赛菲莉亚背包与商店的改动会在重登时被回滚」——写回存档时用错了玩家实例。

## 修正：重生后的改动不再被回滚

**症状**：死亡复活之后放进背包的神器、在商店买下的货、刷新过的货架，**大退重进后会退回「本场会话第一次死亡那一刻」的样子**——背包像少了东西、货架上买过的货重新出现、限购清零，而客户端全程看着一切正常。

**根因**：背包与商店按玩家 UUID 缓存，缓存对象里存着**构造时那个 `ServerPlayer` 实例**，所有写回都走它（缓存只在断线时清）。死亡复活会让服务端换一个**全新的 `ServerPlayer`**（旧实例被移除），Fabric 只按 `copyOnDeath` 把附件复制一份给新实体——之后模组仍然往那个**已经被移除、永远不会再被存档**的旧实例上写，于是从本场第一次死亡起的改动根本没进存档。

**修法**：取背包 / 货架时先确认缓存绑的还是不是当前玩家，不是（且当前玩家没被移除）就换绑，并立刻把内存里最新的一份写回新实体（新实体上那份只是复活时复制的旧快照）。叶子、许愿池、神器技能栏、根之退不受影响——它们每次都直接写当前玩家。之前那段时间里已经丢掉的进度无法找回（它从未写进过磁盘）。

## 未实测

背包扩容、石板升级、锻造、同伴系统尚未实现；大部分内容仍待充分实测，数值与手感继续调整。

---

## English

A patch release fixing "Sephiria Backpack and Shop changes made after a respawn are rolled back on the next login" — the data was being written back through the wrong player instance.

### Fix: changes after a respawn are no longer rolled back

**Symptom**: artifacts put into the backpack, goods bought in the Shop, and refreshed shelves after a death all reverted to **the state at the first death of that session** once you quit and rejoined — the backpack looked short of items, goods already bought reappeared on the shelves with their purchase limits reset, while the client looked perfectly normal the whole time.

**Root cause**: both containers are cached per player UUID as an object that stores **the `ServerPlayer` instance it was built with**, and every save write goes through it (the cache is only cleared on disconnect). Dying replaces the server-side player with a **brand-new `ServerPlayer`** (the old instance is removed). Fabric only **copies** the attachments to the new entity, honouring the `copyOnDeath` flag they were registered with — but the mod kept writing to that **removed instance, which is never saved again**, so everything since the first death of the session never reached the save at all.

**Fix**: when fetching the backpack / shop, check that the cached object is still bound to the current player; if not (and the current player is not removed), rebind it and immediately write the in-memory, newest copy back to the new entity (the copy on the new entity is the stale snapshot taken at respawn). Leaves, the Wishing Pool, artifact skill slots and Root Retreat are unaffected — they always write to the current player directly. Progress already lost in that window cannot be restored (it was never written to disk).

### Not yet play-tested

Backpack expansion, slate upgrades, forging and the companion system are not implemented yet; most content still needs thorough playtesting, and numbers and feel keep being tuned.
