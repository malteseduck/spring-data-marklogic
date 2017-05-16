function updateName(context, params, content) {
    var obj = content.toObject();
    obj.name = "Override Master Read";
    return obj;
}

exports.transform = updateName;