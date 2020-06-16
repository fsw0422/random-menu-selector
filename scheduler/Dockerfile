FROM python:3.6

ENV LC_ALL=C.UTF-8

WORKDIR /usr/src/app

COPY ./requirements.txt .
RUN pip install -r requirements.txt

COPY menu_scheduler.py .

CMD ["python", "menu_scheduler.py"]
