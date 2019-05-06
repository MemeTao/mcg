cookie = nil
local uid=1400300001

request = function()
    if cookie ~= nil then
        uid = uid > 1400301000 and 1400300001 or uid+1
        return wrk.format("POST", string.format("/select?courseids=23,54,45,80,11,30,33&uid=%d",uid))
    else
        return wrk.format("POST", "/login?uid=xxx&token=bbb")
    end
end

response = function(status, headers, body)
   if status == 302 then
      cookie = headers["set-cookie"]
      wrk.headers["cookie"] = cookie
    end
end

