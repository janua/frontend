/* global _: true */
define([
    'modules/vars',
    'modules/authed-ajax',
    'modules/content-api',
    'models/collections/article',
    'utils/clean-clone',
    'utils/deep-get',
    'utils/remove-by-id'
], function(
    vars,
    authedAjax,
    contentApi,
    Article,
    cleanClone,
    deepGet,
    removeById
) {
    var maxChars = vars.CONST.restrictedHeadlineLength || 90,
        restrictHeadlinesOn = vars.CONST.restrictHeadlinesOn || [],
        restrictedLiveMode = vars.CONST.restrictedLiveMode || [];

    function newItemsConstructor(id, sourceItem, targetGroup) {
        var items = [_.extend(_.isObject(sourceItem) ? sourceItem : {}, { id: id })];

        if(sourceItem && sourceItem.meta && sourceItem.meta.supporting) {
            items = items.concat(sourceItem.meta.supporting);
        }

        return _.map(items, function(item) {
            return new Article({
                id: item.id,
                meta: cleanClone(item.meta),
                group: targetGroup
            });
        });
    }

    function newItemsValidator(newItems) {
        var defer = $.Deferred();

        contentApi.validateItem(newItems[0])
        .fail(function(err) {
            defer.reject(err);
        })
        .done(function(item) {
            var front = vars.model.front(),
                err;

            if(item.group.parentType === 'Collection') {
                if (restrictHeadlinesOn.indexOf(front) > -1 && (item.meta.headline() || item.fields.headline()).length > maxChars) {
                    err = 'Sorry, a ' + front + ' headline must be ' + maxChars + ' characters or less. Edit it first within the clipboard.';
                }
                if (restrictedLiveMode.indexOf(front) > -1 && vars.model.liveMode()) {
                    err = 'Sorry, ' + front + ' items cannot be added in Live Front mode. Switch to Draft Front then try again.';
                }
            }

            if (err) {
                defer.reject(err);
            } else {
                defer.resolve(item);
            }
        });

        return defer.promise();
    }

    function newItemsPersister(newItems, sourceGroup, targetGroup, position, isAfter) {
        var id = newItems[0].id(),
            itemMeta,
            supporting,
            remove;

        if (!targetGroup || !targetGroup.parent) { return; }

        if (targetGroup.parentType === 'Article') {
            supporting = targetGroup.parent.meta.supporting.items;
            _.each(newItems.slice(1), function(item) {
                supporting.remove(function (supp) { return supp.id() === item.id(); });
                supporting.push(item);
            });
            contentApi.decorateItems(supporting());
            targetGroup.parent.save();

            remove = remover(sourceGroup, id);
            if (remove) {
                authedAjax.updateCollections({remove: remove});
                removeById(sourceGroup.items, id);
            }
        }

        if (targetGroup.parentType === 'Collection') {
            targetGroup.parent.closeAllArticles();
            itemMeta = newItems[0].getMeta() || {};

            if (deepGet(targetGroup, '.parent.groups.length') > 1) {
                itemMeta.group = targetGroup.index + '';
            } else {
                delete itemMeta.group;
            }

            remove = (deepGet(sourceGroup, '.parent.id') && deepGet(sourceGroup, '.parent.id') !== targetGroup.parent.id);
            remove = remove ? remover(sourceGroup, id) : undefined;

            authedAjax.updateCollections({
                update: {
                    collection: targetGroup.parent,
                    item:     id,
                    position: position,
                    after:    isAfter,
                    live:     vars.state.liveMode(),
                    draft:   !vars.state.liveMode(),
                    itemMeta: _.isEmpty(itemMeta) ? undefined : itemMeta
                },
                remove: remove
            })
            .then(function() {
                if(vars.state.liveMode()) {
                    vars.model.deferredDetectPressFailure();
                }
            });

            if (sourceGroup && !sourceGroup.keepCopy && sourceGroup !== targetGroup) {
                removeById(sourceGroup.items, id); // for immediate UI effect
            }
        }
    }

    function remover(sourceGroup, id) {
        if (sourceGroup &&
            sourceGroup.parentType === 'Collection' &&
           !sourceGroup.keepCopy) {

            return {
                collection: sourceGroup.parent,
                id:     sourceGroup.parent.id,
                item:   id,
                live:   vars.state.liveMode(),
                draft: !vars.state.liveMode()
            };
        }
    }

    return {
        newItemsConstructor: newItemsConstructor,
        newItemsValidator: newItemsValidator,
        newItemsPersister: newItemsPersister
    };
});
