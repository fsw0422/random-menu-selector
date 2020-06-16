from random import randrange
from requests import get, post
from time import sleep
import schedule

HOST = "https://rms.fsw0422.com"


def job():
    res = get(HOST + "/v1/menu/view", params={"name": ""})
    menu_uuids = [r["uuid"] for r in res.json()["result"]]
    rand_menu_idx = randrange(len(menu_uuids))
    post(HOST + "/v1/menu/random", json={"uuid": menu_uuids[rand_menu_idx]})


if __name__ == "__main__":
    schedule.every().day.at("12:30").do(job)
    while True:
        schedule.run_pending()
        sleep(60)
